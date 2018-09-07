package asura.core.es.service

import asura.common.util.{JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryScenario
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, delete, indexInto, _}
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ScenarioService extends CommonService {

  def index(s: Scenario): Future[IndexDocResponse] = {
    val error = check(s)
    if (null == error) {
      EsClient.httpClient.execute {
        indexInto(Scenario.Index / EsConfig.DefaultType).doc(s).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    EsClient.httpClient.execute {
      delete(id).from(Scenario.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
    }.map(toDeleteDocResponse(_))
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    EsClient.httpClient.execute {
      bulk(ids.map(id => delete(id).from(Scenario.Index / EsConfig.DefaultType)))
    }.map(toBulkDocResponse(_))
  }

  def getById(id: String) = {
    EsClient.httpClient.execute {
      search(Scenario.Index).query(idsQuery(id)).size(1)
    }
  }

  def getByIds(ids: Seq[String]) = {
    EsClient.httpClient.execute {
      search(Scenario.Index).query(idsQuery(ids)).size(ids.length).sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateScenario(id: String, s: Scenario): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      Future.failed(new IllegalArgumentException("empty id"))
    } else {
      EsClient.httpClient.execute {
        update(id).in(Scenario.Index / EsConfig.DefaultType).doc(JsonUtils.stringify(s.toUpdateMap))
      }.map(toUpdateDocResponse(_))
    }
  }

  /**
    * Seq({id->scenario})
    */
  def getScenariosByIds(ids: Seq[String]): Future[Seq[(String, Scenario)]] = {
    getByIds(ids).map(res => {
      res match {
        case Right(success) =>
          if (success.result.isEmpty) {
            Nil
          } else {
            success.result.hits.hits.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Scenario])))
          }
        case Left(failure) =>
          throw ErrorMessages.error_EsRequestFail(failure).toException
      }
    })
  }

  def queryScenario(query: QueryScenario) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(query.group)) queryDefinitions += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) queryDefinitions += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.text)) queryDefinitions += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    EsClient.httpClient.execute {
      search(Scenario.Index).query(boolQuery().must(queryDefinitions))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def check(s: Scenario): ErrorMessages.Val = {
    if (null == s) {
      ErrorMessages.error_EmptyScenario
    } else if (StringUtils.isEmpty(s.summary)) {
      ErrorMessages.error_EmptySummary
    } else if (StringUtils.isEmpty(s.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(s.group)) {
      ErrorMessages.error_EmptyGroup
    } else {
      null
    }
  }
}
