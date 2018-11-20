package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CommonValidator
import asura.core.cs.model.QueryGroup
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object GroupService extends CommonService {

  val logger = Logger("GroupService")

  def index(group: Group): Future[IndexDocResponse] = {
    if (!CommonValidator.isIdLegal(group.id)) {
      ErrorMessages.error_IllegalGroupId.toFutureFail
    } else {
      docExists(group.id).flatMap(isExist => {
        if (isExist.isSuccess) {
          if (isExist.result) {
            ErrorMessages.error_GroupExists.toFutureFail
          } else {
            EsClient.esClient.execute {
              indexInto(Group.Index / EsConfig.DefaultType).doc(group).id(group.id).refresh(RefreshPolicy.WAIT_UNTIL)
            }.map(toIndexDocResponse(_))
          }
        } else {
          ErrorMessages.error_EsRequestFail(isExist).toFutureFail
        }
      })
    }
  }

  def deleteGroup(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      IndexService.deleteByGroupOrProject(Seq(
        Case.Index, RestApi.Index, Job.Index, Environment.Index,
        JobReport.Index, JobNotify.Index, Project.Index, Scenario.Index, Activity.Index
      ), id, null).flatMap(idxRes => {
        EsClient.esClient.execute {
          delete(id).from(Group.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(_ => idxRes)
      })
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Group.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getMaxGroups(): Future[Seq[Group]] = {
    EsClient.esClient.execute {
      search(Group.Index)
        .query(matchAllQuery())
        .limit(EsConfig.MaxCount)
        .sourceInclude(FieldKeys.FIELD_ID, FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION, FieldKeys.FIELD_AVATAR)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }.map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.map(hit => JacksonSupport.parse(hit.sourceAsString, classOf[Group]))
        }
      } else {
        throw RequestFailException(res.error.reason)
      }
    })
  }

  def updateGroup(group: Group): Future[UpdateDocResponse] = {
    if (null == group || null == group.id) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(group.id).in(Group.Index / EsConfig.DefaultType).doc(group.toUpdateMap)
      }
    }.map(toUpdateDocResponse(_))
  }

  def docExists(id: String) = {
    EsClient.esClient.execute {
      exists(id, Group.Index, EsConfig.DefaultType)
    }
  }

  def queryGroup(query: QueryGroup) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.id)) esQueries += wildcardQuery(FieldKeys.FIELD_ID, query.id + "*")
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    EsClient.esClient.execute {
      search(Group.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_ID :+ FieldKeys.FIELD_AVATAR)
    }
  }
}
