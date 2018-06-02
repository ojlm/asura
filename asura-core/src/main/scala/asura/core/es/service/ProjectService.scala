package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CommonValidator
import asura.core.es.model.{FieldKeys, Project}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._

object ProjectService {

  def index(project: Project) = {
    if (null == project || StringUtils.isEmpty(project.group)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else if (!CommonValidator.isIdLegal(project.id)) {
      FutureUtils.illegalArgs(ApiMsg.ILLEGAL_CHARACTER)
    } else {
      docCount(project.group, project.id).flatMap {
        case Right(countRes) =>
          if (countRes.result.count > 0) {
            FutureUtils.illegalArgs("Project already exists")
          } else {
            EsClient.httpClient.execute {
              indexInto(Project.Index / EsConfig.DefaultType).doc(project).id(project.id).refresh(RefreshPolicy.WAIT_UNTIL)
            }
          }
        case Left(error) =>
          FutureUtils.requestFail(error.error.reason)
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

  def deleteDoc(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        bulk(ids.map(id => delete(id).from(Project.Index)))
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Project.Index).query(idsQuery(id))
      }
    }
  }

  def getAll(group: String) = {
    if (StringUtils.isEmpty(group)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Project.Index)
          .query(termQuery(FieldKeys.FIELD_GROUP, group))
          .limit(EsConfig.MaxCount)
          .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
      }
    }
  }

  def updateProject(project: Project) = {
    if (null == project || StringUtils.isEmpty(project.id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
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
}
