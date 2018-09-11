package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CommonValidator
import asura.core.cs.model.QueryProject
import asura.core.es.model.{FieldKeys, IndexDocResponse, Project, UpdateDocResponse}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ProjectService extends CommonService {

  def index(project: Project): Future[IndexDocResponse] = {
    if (null == project || StringUtils.isEmpty(project.group)) {
      ErrorMessages.error_IllegalGroupId.toFutureFail
    } else if (!CommonValidator.isIdLegal(project.id)) {
      ErrorMessages.error_IllegalProjectId.toFutureFail
    } else {
      docCount(project.group, project.id).flatMap(res => {
        if (res.isSuccess) {
          if (res.result.count > 0) {
            ErrorMessages.error_ProjectExists.toFutureFail
          } else {
            EsClient.esClient.execute {
              indexInto(Project.Index / EsConfig.DefaultType)
                .doc(project)
                .id(project.generateDocId())
                .refresh(RefreshPolicy.WAIT_UNTIL)
            }.map(toIndexDocResponse(_))
          }
        } else {
          ErrorMessages.error_EsRequestFail(res).toFutureFail
        }
      })
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Project.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }
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
        update(Project.generateDocId(group, projectId)).in(Project.Index / EsConfig.DefaultType).doc(Map(FieldKeys.FIELD_OPENAPI -> openapi))
      }.map(toUpdateDocResponse(_))
    }
  }

  def updateProject(project: Project): Future[UpdateDocResponse] = {
    if (null == project || StringUtils.isEmpty(project.group) || StringUtils.isEmpty(project.id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(project.generateDocId()).in(Project.Index / EsConfig.DefaultType).doc(project.toUpdateMap)
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
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.id)) esQueries += wildcardQuery(FieldKeys.FIELD_ID, query.id + "*")
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    EsClient.esClient.execute {
      search(Project.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_GROUP :+ FieldKeys.FIELD_ID :+ FieldKeys.FIELD_AVATAR)
    }
  }
}
