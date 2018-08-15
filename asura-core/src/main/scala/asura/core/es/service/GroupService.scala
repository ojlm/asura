package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryGroup
import asura.core.es.model.{FieldKeys, Group, IndexDocResponse}
import asura.core.es.{EsClient, EsConfig}
import asura.core.exceptions.IndexDocFailException
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object GroupService extends CommonService {

  val logger = Logger("GroupService")

  def index(group: Group): Future[IndexDocResponse] = {
    if (StringUtils.isEmpty(group.id)) {
      FutureUtils.illegalArgs("Empty group id")
    } else {
      docExists(group.id).flatMap(isExist => {
        isExist match {
          case Right(_) =>
            EsClient.httpClient.execute {
              indexInto(Group.Index / EsConfig.DefaultType).doc(group).id(group.id).refresh(RefreshPolicy.WAIT_UNTIL)
            }.map(toIndexDocResponse(_))
          case Left(_) =>
            FutureUtils.illegalArgs("Group already exists")
        }
      })
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(Group.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Group.Index).query(idsQuery(id))
      }
    }
  }

  def getAll() = {
    EsClient.httpClient.execute {
      search(Group.Index)
        .query(matchAllQuery())
        .limit(EsConfig.MaxCount)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateGroup(group: Group) = {
    if (null == group || null == group.id) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        update(group.id).in(Group.Index / EsConfig.DefaultType).doc(group.toUpdateMap)
      }
    }
  }

  def docExists(id: String) = {
    EsClient.httpClient.execute {
      exists(id, Group.Index, EsConfig.DefaultType)
    }
  }

  def queryGroup(query: QueryGroup) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(query.text)) queryDefinitions += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    EsClient.httpClient.execute {
      search(Group.Index).query(boolQuery().must(queryDefinitions))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }
}
