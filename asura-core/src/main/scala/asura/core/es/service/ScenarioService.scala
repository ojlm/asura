package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.BoolErrorRes
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.cs.model.QueryScenario
import asura.core.es.model.{FieldKeys, Scenario}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, delete, indexInto, _}
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object ScenarioService {

  def index(s: Scenario) = {
    val (isOk, errMsg) = check(s)
    if (isOk) {
      EsClient.httpClient.execute {
        indexInto(Scenario.Index / EsConfig.DefaultType).doc(s).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    } else {
      Future.failed(new IllegalArgumentException(errMsg))
    }
  }

  def deleteDoc(id: String) = {
    EsClient.httpClient.execute {
      delete(id).from(Scenario.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
    }
  }

  def deleteDoc(ids: Seq[String]) = {
    EsClient.httpClient.execute {
      bulk(ids.map(id => delete(id).from(Scenario.Index / EsConfig.DefaultType)))
    }
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


  def updateScenario(id: String, s: Scenario) = {
    if (StringUtils.isEmpty(id)) {
      Future.failed(new IllegalArgumentException("empty id"))
    } else {
      EsClient.httpClient.execute {
        update(id).in(Scenario.Index / EsConfig.DefaultType).doc(JsonUtils.stringify(s.toUpdateMap))
      }
    }
  }

  /**
    * Seq({id->scenario})
    */
  def getScenariosByIds(ids: Seq[String])(implicit executor: ExecutionContext): Future[Seq[(String, Scenario)]] = {
    getByIds(ids).map(res => {
      res match {
        case Right(success) =>
          if (success.result.isEmpty) {
            throw IllegalRequestException(s"ids: ${ids.mkString(",")} not found.")
          } else {
            success.result.hits.hits.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Scenario])))
          }
        case Left(failure) =>
          throw RequestFailException(failure.error.reason)
      }
    })
  }

  def searchText(text: String) = {
    EsClient.httpClient.execute {
      search(Scenario.Index).query {
        matchQuery(FieldKeys.FIELD__TEXT, text)
      }.sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
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

  def check(s: Scenario): BoolErrorRes = {
    if (null == s) {
      (false, "empty scenario")
    } else if (StringUtils.isEmpty(s.project)) {
      (false, "empty project")
    } else if (StringUtils.isEmpty(s.group)) {
      (false, "empty group")
    } else {
      (true, null)
    }
  }
}
