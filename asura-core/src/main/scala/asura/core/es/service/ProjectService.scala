package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.model.{QueryProject, TransferProject}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.core.util.{CommonValidator, JacksonSupport}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.sksamuel.elastic4s.requests.update.UpdateByQueryRequest

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Iterable, mutable}
import scala.concurrent.Future

object ProjectService extends CommonService {

  val projectRelatedIndexes = Seq(
    HttpCaseRequest.Index, Job.Index, Environment.Index,
    JobReport.Index, JobNotify.Index, Scenario.Index, Activity.Index,
    ProjectApiCoverage.Index, DubboRequest.Index, SqlRequest.Index, CiTrigger.Index, Favorite.Index
  )

  def index(project: Project, checkExists: Boolean = true): Future[IndexDocResponse] = {
    if (null == project || StringUtils.isEmpty(project.group)) {
      ErrorMessages.error_IllegalGroupId.toFutureFail
    } else if (!CommonValidator.isIdLegal(project.id)) {
      ErrorMessages.error_IllegalProjectId.toFutureFail
    } else {
      if (checkExists) {
        docCount(project.group, project.id).flatMap(res => {
          if (res.isSuccess) {
            if (res.result.count > 0) {
              ErrorMessages.error_ProjectExists.toFutureFail
            } else {
              EsClient.esClient.execute {
                indexInto(Project.Index)
                  .doc(project)
                  .id(project.generateDocId())
                  .refresh(RefreshPolicy.WAIT_FOR)
              }.map(toIndexDocResponse(_))
            }
          } else {
            ErrorMessages.error_EsRequestFail(res).toFutureFail
          }
        })
      } else {
        EsClient.esClient.execute {
          indexInto(Project.Index)
            .doc(project)
            .id(project.generateDocId())
            .refresh(RefreshPolicy.WAIT_FOR)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteProject(group: String, project: String) = {
    if (StringUtils.isEmpty(group) && StringUtils.isNotEmpty(project)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      IndexService.deleteByGroupOrProject(projectRelatedIndexes, group, project).flatMap(idxRes => {
        EsClient.esClient.execute {
          delete(Project.generateDocId(group, project)).from(Project.Index).refresh(RefreshPolicy.WAIT_FOR)
        }.map(_ => idxRes)
      })
    }
  }

  // ugly and dangerous and unpredictable code
  def transferProject(op: TransferProject) = {
    if (
      StringUtils.isNotEmpty(op.oldGroup) && StringUtils.isNotEmpty(op.newGroup) &&
        StringUtils.isNotEmpty(op.oldId) && StringUtils.isNotEmpty(op.newId) &&
        CommonValidator.isIdLegal(op.newGroup) && CommonValidator.isIdLegal(op.newId)
    ) {
      docCount(op.newGroup, op.newId).flatMap(res => {
        if (res.isSuccess) {
          if (res.result.count > 0) {
            ErrorMessages.error_ProjectExists.toFutureFail
          } else {
            val esQueries = Seq(termQuery(FieldKeys.FIELD_GROUP, op.oldGroup), termQuery(FieldKeys.FIELD_PROJECT, op.oldId))
            EsClient.esClient.execute {
              UpdateByQueryRequest(
                indexes = Indexes(projectRelatedIndexes),
                query = boolQuery().must(esQueries),
                script = Option(Script(
                  script = s"ctx._source.${FieldKeys.FIELD_GROUP} = '${op.newGroup}';ctx._source.${FieldKeys.FIELD_PROJECT} = '${op.newId}'",
                  lang = Option("painless")
                ))
              ).refreshImmediately
            }.flatMap(_ => {
              getById(op.oldGroup, op.oldId, true)
            }).flatMap(esRes => {
              if (esRes.isSuccess) {
                var newProject = esRes.result.hits.hits(0).sourceAsMap
                newProject += (FieldKeys.FIELD_GROUP -> op.newGroup)
                newProject += (FieldKeys.FIELD_ID -> op.newId)
                // delete the old doc
                EsClient.esClient.execute {
                  delete(Project.generateDocId(op.oldGroup, op.oldId))
                    .from(Project.Index)
                    .refresh(RefreshPolicy.WAIT_FOR)
                }.flatMap(_ => {
                  // insert the new doc
                  EsClient.esClient.execute {
                    indexInto(Project.Index)
                      .doc(newProject)
                      .id(Project.generateDocId(op.newGroup, op.newId))
                      .refresh(RefreshPolicy.WAIT_FOR)
                  }.map(_ => op)
                })
              } else {
                ErrorMessages.error_EsRequestFail(esRes).toFutureFail
              }
            })
          }
        } else {
          ErrorMessages.error_EsRequestFail(res).toFutureFail
        }
      })
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  def getById(group: String, id: String, includeOpenapi: Boolean = false) = {
    if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_GroupIdEmpty.toFutureFail
    } else if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      val esQueries = ArrayBuffer[Query]()
      esQueries += matchQuery(FieldKeys.FIELD_ID, id)
      esQueries += matchQuery(FieldKeys.FIELD_GROUP, group)
      EsClient.esClient.execute {
        if (includeOpenapi) {
          search(Project.Index).query(boolQuery().must(esQueries)).size(1)
        } else {
          search(Project.Index).query(boolQuery().must(esQueries)).size(1).sourceExclude(FieldKeys.FIELD_OPENAPI)
        }
      }
    }
  }

  def getByIds(ids: Seq[String], source: Seq[String] = Nil): Future[Map[String, Project]] = {
    if (null == ids || ids.isEmpty) {
      Future.successful(Map.empty)
    } else {
      EsClient.esClient.execute {
        search(Project.Index)
          .query(idsQuery(ids))
          .from(0)
          .size(ids.length)
          .sourceInclude(source)
      }.map(res => {
        if (res.isSuccess) {
          val map = mutable.Map[String, Project]()
          if (res.result.nonEmpty) {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[Project])))
          }
          map.toMap
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    }
  }

  def getOpenApi(group: String, projectId: String) = {
    if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_GroupIdEmpty.toFutureFail
    } else if (StringUtils.isEmpty(projectId)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      val esQueries = ArrayBuffer[Query]()
      esQueries += matchQuery(FieldKeys.FIELD_ID, projectId)
      esQueries += matchQuery(FieldKeys.FIELD_GROUP, group)
      EsClient.esClient.execute {
        search(Project.Index).query(boolQuery().must(esQueries)).size(1).sourceInclude(FieldKeys.FIELD_OPENAPI)
      }
    }
  }

  def updateOpenApi(group: String, projectId: String, openapi: String) = {
    if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_GroupIdEmpty.toFutureFail
    } else if (StringUtils.isEmpty(projectId)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(Project.generateDocId(group, projectId)).in(Project.Index).doc(Map(FieldKeys.FIELD_OPENAPI -> openapi))
      }.map(toUpdateDocResponse(_))
    }
  }

  def updateProject(project: Project): Future[UpdateDocResponse] = {
    if (null == project || StringUtils.isEmpty(project.group) || StringUtils.isEmpty(project.id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(project.generateDocId()).in(Project.Index).doc(project.toUpdateMap)
      }.map(toUpdateDocResponse(_))
    }
  }

  def docCount(group: String, id: String) = {
    EsClient.esClient.execute {
      count(Project.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, group),
          termQuery(FieldKeys.FIELD_ID, id)
        )
      }
    }
  }

  def queryProject(query: QueryProject) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.id)) esQueries += wildcardQuery(FieldKeys.FIELD_ID, s"*${query.id}*")
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    EsClient.esClient.execute {
      search(Project.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_GROUP :+ FieldKeys.FIELD_ID :+ FieldKeys.FIELD_AVATAR)
    }
  }

  // returns max 1000 doc
  def getProjectsByDomain(domain: String): Future[Seq[Project]] = {
    EsClient.esClient.execute {
      search(Project.Index).query(nestedQuery(FieldKeys.FIELD_DOMAINS, termQuery(FieldKeys.FIELD_NESTED_DOMAINS_NAME, domain)))
        .size(EsConfig.MaxCount)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(FieldKeys.FIELD_GROUP, FieldKeys.FIELD_ID)
    }.map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.toIndexedSeq.map(hit => JacksonSupport.parse(hit.sourceAsString, classOf[Project]))
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(Project.Index).query(idsQuery(ids)).size(ids.size).sourceExclude(Seq(
          FieldKeys.FIELD_OPENAPI, FieldKeys.FIELD_DOMAINS
        ))
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }
}
