package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CommonValidator
import asura.core.cs.model.QueryProject
import asura.core.es.model.{FieldKeys, IndexDocResponse, Project}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ProjectService extends CommonService {

  def index(project: Project): Future[IndexDocResponse] = {
    if (null == project || StringUtils.isEmpty(project.group)) {
      ErrorMessages.error_IllegalGroupId.toFutureFail
    } else if (!CommonValidator.isIdLegal(project.id)) {
      ErrorMessages.error_IllegalProjectId.toFutureFail
    } else {
      docCount(project.group, project.id).flatMap {
        case Right(countRes) =>
          if (countRes.result.count > 0) {
            ErrorMessages.error_ProjectExists.toFutureFail
          } else {
            EsClient.httpClient.execute {
              indexInto(Project.Index / EsConfig.DefaultType)
                .doc(project)
                .id(s"${project.group}_${project.id}")
                .refresh(RefreshPolicy.WAIT_UNTIL)
            }.map(toIndexDocResponse(_))
          }
        case Left(failure) =>
          ErrorMessages.error_EsRequestFail(failure).toFutureFail
      }
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(Project.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def getById(group: String, id: String) = {
    if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_GroupIdEmpty.toFutureFail
    } else if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      // not use groupID_projectId for old version back compatible
      val queryDefinitions = ArrayBuffer[QueryDefinition]()
      queryDefinitions += matchQuery(FieldKeys.FIELD_ID, id)
      queryDefinitions += matchQuery(FieldKeys.FIELD_GROUP, group)
      EsClient.httpClient.execute {
        search(Project.Index).query(boolQuery().must(queryDefinitions)).size(1)
      }
    }
  }

  def updateProject(project: Project) = {
    if (null == project || StringUtils.isEmpty(project.id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      EsClient.httpClient.execute {
        update(project.id).in(Project.Index / EsConfig.DefaultType).doc(project.toUpdateMap)
      }
    }
  }

  def docCount(group: String, id: String) = {
    EsClient.httpClient.execute {
      count(Project.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, group),
          termQuery(FieldKeys.FIELD_ID, id)
        )
      }
    }
  }

  def queryProject(query: QueryProject) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(query.id)) queryDefinitions += wildcardQuery(FieldKeys.FIELD_ID, query.id + "*")
    if (StringUtils.isNotEmpty(query.text)) queryDefinitions += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    if (StringUtils.isNotEmpty(query.group)) queryDefinitions += termQuery(FieldKeys.FIELD_GROUP, query.group)
    EsClient.httpClient.execute {
      search(Project.Index).query(boolQuery().must(queryDefinitions))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_ID :+ FieldKeys.FIELD_AVATAR)
    }
  }
}
