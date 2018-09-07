package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CaseValidator
import asura.core.cs.model.QueryCase
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, delete, indexInto, _}
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object CaseService extends CommonService {

  def index(cs: Case): Future[IndexDocResponse] = {
    val error = CaseValidator.check(cs)
    if (null == error) {
      EsClient.httpClient.execute {
        indexInto(Case.Index / EsConfig.DefaultType).doc(cs).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def index(css: Seq[Case]): Future[BulkIndexDocResponse] = {
    val error = CaseValidator.check(css)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.httpClient.execute {
        bulk(
          css.map(cs => indexInto(Case.Index / EsConfig.DefaultType).doc(cs))
        )
      }.map(toBulkIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    EsClient.httpClient.execute {
      delete(id).from(Case.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
    }.map(toDeleteDocResponse(_))
  }

  def deleteDoc(ids: Seq[String]): Future[DeleteDocResponse] = {
    EsClient.httpClient.execute {
      bulk(ids.map(id => delete(id).from(Case.Index / EsConfig.DefaultType)))
    }.map(toDeleteDocResponseFromBulk(_))
  }

  def getById(id: String) = {
    EsClient.httpClient.execute {
      search(Case.Index).query(idsQuery(id)).size(1)
    }
  }

  def getByIds(ids: Seq[String]) = {
    EsClient.httpClient.execute {
      search(Case.Index).query(idsQuery(ids)).from(0).size(ids.length).sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def getByApi(api: String, project: String) = {
    EsClient.httpClient.execute {
      search(Case.Index).query {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_API, api),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }.sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateCs(id: String, cs: Case) = {
    if (StringUtils.isEmpty(id)) {
      Future.failed(new IllegalArgumentException("empty id"))
    } else {
      val error = CaseValidator.check(cs)
      if (null == error) {
        EsClient.httpClient.execute {
          val (src, params) = cs.toUpdateScriptParams
          update(id).in(Case.Index / EsConfig.DefaultType).script {
            script(src).params(params)
          }
        }
      } else {
        error.toFutureFail
      }
    }
  }

  /**
    * Seq({id->case})
    */
  def getCasesByIds(ids: Seq[String])(implicit executor: ExecutionContext): Future[Seq[(String, Case)]] = {
    getByIds(ids).map(res => {
      res match {
        case Right(success) =>
          if (success.result.isEmpty) {
            throw IllegalRequestException(s"ids: ${ids.mkString(",")} not found.")
          } else {
            success.result.hits.hits.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
          }
        case Left(failure) =>
          throw RequestFailException(failure.error.reason)
      }
    })
  }

  def getCasesByIdsAsMap(ids: Seq[String])(implicit executor: ExecutionContext): Future[Map[String, Case]] = {
    val map = mutable.HashMap[String, Case]()
    getByIds(ids).map(res => {
      res match {
        case Right(success) =>
          if (success.result.isEmpty) {
            throw IllegalRequestException(s"ids: ${ids.mkString(",")} not found.")
          } else {
            success.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
            map.toMap
          }
        case Left(failure) =>
          throw RequestFailException(failure.error.reason)
      }
    })
  }

  def searchText(text: String) = {
    EsClient.httpClient.execute {
      search(Case.Index).query {
        matchQuery(FieldKeys.FIELD__TEXT, text)
      }.sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def queryCase(query: QueryCase) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(query.group)) queryDefinitions += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) queryDefinitions += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.api)) queryDefinitions += termQuery(FieldKeys.FIELD_API, query.api)
    if (StringUtils.isNotEmpty(query.text)) queryDefinitions += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    if (StringUtils.isNotEmpty(query.path)) queryDefinitions += wildcardQuery(FieldKeys.FIELD_NESTED_REQUEST_URLPATH, s"${query.path}*")
    EsClient.httpClient.execute {
      search(Case.Index).query(boolQuery().must(queryDefinitions))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }
}
