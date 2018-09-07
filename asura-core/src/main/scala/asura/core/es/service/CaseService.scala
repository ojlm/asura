package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CaseValidator
import asura.core.cs.model.QueryCase
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
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

  def index(css: Seq[Case]): Future[BulkDocResponse] = {
    val error = CaseValidator.check(css)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.httpClient.execute {
        bulk(
          css.map(cs => indexInto(Case.Index / EsConfig.DefaultType).doc(cs))
        )
      }.map(toBulkDocResponse(_))
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

  private def getByIds(ids: Seq[String]) = {
    if (null != ids) {
      EsClient.httpClient.execute {
        search(Case.Index).query(idsQuery(ids)).from(0).size(ids.length).sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
      }
    } else {
      ErrorMessages.error_EmptyId.toFutureFail
    }
  }

  def updateCs(id: String, cs: Case): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = CaseValidator.check(cs)
      if (null == error) {
        EsClient.httpClient.execute {
          val (src, params) = cs.toUpdateScriptParams
          update(id).in(Case.Index / EsConfig.DefaultType).script {
            script(src).params(params)
          }
        }.map(toUpdateDocResponse(_))
      } else {
        error.toFutureFail
      }
    }
  }

  /**
    * Seq({id->case})
    */
  def getCasesByIds(ids: Seq[String])(implicit executor: ExecutionContext): Future[Seq[(String, Case)]] = {
    if (null != ids && ids.nonEmpty) {
      getByIds(ids).map(res => {
        res match {
          case Right(success) =>
            if (success.result.isEmpty) {
              throw ErrorMessages.error_IdsNotFound(ids).toException
            } else {
              success.result.hits.hits.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
            }
          case Left(failure) =>
            throw RequestFailException(failure.error.reason)
        }
      })
    } else {
      Future.successful(Nil)
    }
  }

  def getCasesByIdsAsMap(ids: Seq[String])(implicit executor: ExecutionContext): Future[Map[String, Case]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, Case]()
      getByIds(ids).map(res => {
        res match {
          case Right(success) =>
            if (success.result.isEmpty) {
              throw ErrorMessages.error_IdsNotFound(ids).toException
            } else {
              success.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
              map.toMap
            }
          case Left(failure) =>
            throw ErrorMessages.error_EsRequestFail(failure).toException
        }
      })
    } else {
      ErrorMessages.error_EmptyId.toFutureFail
    }
  }

  def queryCase(query: QueryCase): Future[Map[String, Any]] = {
    if (null != query.ids && query.ids.nonEmpty) {
      getByIds(query.ids).map(res => {
        res match {
          case Right(success) => {
            val idMap = scala.collection.mutable.HashMap[String, Any]()
            success.result.hits.hits.foreach(hit => {
              idMap += (hit.id -> (hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)))
            })
            Map("total" -> success.result.hits.total, "list" -> query.ids.map(id => idMap(id)))
          }
          case Left(failure) => throw ErrorMessages.error_EsRequestFail(failure).toException
        }
      })
    } else {
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
      }.map(res => {
        res match {
          case Right(success) => EsResponse.toApiData(success.result)
          case Left(failure) => throw ErrorMessages.error_EsRequestFail(failure).toException
        }
      })
    }
  }
}
