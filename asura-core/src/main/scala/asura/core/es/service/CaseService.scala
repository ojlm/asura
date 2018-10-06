package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.CaseValidator
import asura.core.cs.model.{QueryCase, QueryHistory}
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, delete, indexInto, _}
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object CaseService extends CommonService {

  val queryFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_NESTED_REQUEST_URLPATH,
    FieldKeys.FIELD_NESTED_REQUEST_METHOD,
  )

  def index(cs: Case): Future[IndexDocResponse] = {
    val error = CaseValidator.check(cs)
    if (null == error) {
      cs.calcGeneratorCount()
      EsClient.esClient.execute {
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
      EsClient.esClient.execute {
        bulk(
          css.map(cs => {
            cs.calcGeneratorCount()
            indexInto(Case.Index / EsConfig.DefaultType).doc(cs)
          })
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      delete(id).from(Case.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
    }.map(toDeleteDocResponse(_))
  }

  def deleteDoc(ids: Seq[String]): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      bulk(ids.map(id => delete(id).from(Case.Index / EsConfig.DefaultType)))
    }.map(toDeleteDocResponseFromBulk(_))
  }

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(Case.Index).query(idsQuery(id)).size(1)
    }
  }

  private def getByIds(ids: Seq[String], filterFields: Boolean = false) = {
    if (null != ids) {
      EsClient.esClient.execute {
        val clause = search(Case.Index)
          .query(idsQuery(ids))
          .from(0)
          .size(ids.length)
          .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
        if (!filterFields) {
          clause.sourceInclude(queryFields)
        }
        clause
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
        cs.calcGeneratorCount()
        EsClient.esClient.execute {
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
    *
    * @param filterFields if false return all fields of doc, other only return filed in [[queryFields]]
    */
  def getCasesByIds(ids: Seq[String], filterFields: Boolean = false)(implicit executor: ExecutionContext): Future[Seq[(String, Case)]] = {
    if (null != ids && ids.nonEmpty) {
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
          }
        } else {
          throw RequestFailException(res.error.reason)
        }
      })
    } else {
      Future.successful(Nil)
    }
  }

  def getCasesByIdsAsMap(ids: Seq[String], filterFields: Boolean = false)(implicit executor: ExecutionContext): Future[Map[String, Case]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, Case]()
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[Case])))
            map.toMap
          }
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  /**
    * return Map("total" -> total , "list" -> list)
    */
  def queryCase(query: QueryCase): Future[Map[String, Any]] = {
    if (null != query.ids && query.ids.nonEmpty) {
      getByIds(query.ids).map(res => {
        if (res.isSuccess) {
          val idMap = scala.collection.mutable.HashMap[String, Any]()
          res.result.hits.hits.foreach(hit => {
            idMap += (hit.id -> (hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)))
          })
          Map("total" -> res.result.hits.total, "list" -> query.ids.map(id => idMap(id)))
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
      if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
      if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      if (StringUtils.isNotEmpty(query.path)) esQueries += wildcardQuery(FieldKeys.FIELD_NESTED_REQUEST_URLPATH, s"${query.path}*")
      EsClient.esClient.execute {
        search(Case.Index).query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
          .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
          .sourceInclude(queryFields)
      }.map(res => {
        if (res.isSuccess) {
          EsResponse.toApiData(res.result)
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    }
  }

  def queryHistory(query: QueryHistory) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    EsClient.esClient.execute {
      val clause = search(Case.Index)
        .query(boolQuery().must(esQueries))
        .sortBy(FieldSort(FieldKeys.FIELD_CREATED_AT).desc(), FieldSort(FieldKeys.FIELD__ID).desc())
      if (StringUtils.isNotEmpty(query.id) && StringUtils.isNotEmpty(query.createdAt)) {
        clause.searchAfter(Seq(query.createdAt, query.id))
      }
      clause
    }.map { res =>
      if (res.isSuccess) {
        EsResponse.toApiData(res.result)
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    }
  }
}
