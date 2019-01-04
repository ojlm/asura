package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryOnlineApi
import asura.core.es.model.{BulkDocResponse, FieldKeys, RestApiOnlineLog}
import asura.core.es.service.CommonService.CustomCatIndices
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl.{termQuery, _}
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


object RestApiOnlineLogService extends CommonService {

  def index(items: Seq[RestApiOnlineLog], day: String): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(s"${RestApiOnlineLog.Index}-${day}" / EsConfig.DefaultType).doc(item))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def getIndices() = {
    EsClient.esClient.execute {
      CustomCatIndices(s"${RestApiOnlineLog.Index}-*")
    }
  }

  def queryOnlineApiLog(query: QueryOnlineApi) = {
    if (StringUtils.isEmpty(query.date)) {
      ErrorMessages.error_EmptyDate.toFutureFail
    } else {
      val sort = if (StringUtils.isNotEmpty(query.sortField)) {
        if (query.asc) {
          FieldSort(query.sortField).asc()
        } else {
          FieldSort(query.sortField).desc()
        }
      } else {
        if (query.asc) {
          FieldSort(FieldKeys.FIELD_COUNT).asc()
        } else {
          FieldSort(FieldKeys.FIELD_COUNT).desc()
        }
      }
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.domain)) esQueries += termQuery(FieldKeys.FIELD_DOMAIN, query.domain)
      if (null != query.tag) esQueries += termQuery(FieldKeys.FIELD_TAG, query.tag)
      if (StringUtils.isNotEmpty(query.method)) esQueries += termQuery(FieldKeys.FIELD_METHOD, query.method)
      if (StringUtils.isNotEmpty(query.urlPath)) esQueries += wildcardQuery(FieldKeys.FIELD_URL_PATH, s"${query.urlPath}*")
      EsClient.esClient.execute {
        search(s"${RestApiOnlineLog.Index}-${query.date}").query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
          .sortBy(sort)
      }
    }
  }

  def getOnlineApiMetrics(query: QueryOnlineApi): Future[Seq[ApiMetrics]] = {
    if (null != query && StringUtils.isNotEmpty(query.domain)) {
      EsClient.esClient.execute {
        search(s"${RestApiOnlineLog.Index}-*")
          .query(boolQuery().must(
            termQuery(FieldKeys.FIELD_DOMAIN, query.domain),
            termQuery(FieldKeys.FIELD_METHOD, query.method),
            termQuery(FieldKeys.FIELD_URL_PATH, query.urlPath)
          ))
          .size(query.pageSize)
          .sourceInclude(FieldKeys.FIELD_METRICS)
      }.map(res => {
        if (res.isSuccess) {
          res.result.hits.hits.map(hit => {
            val dateIndex = hit.index
            val offset = dateIndex.length - RestApiOnlineLog.INDEX_DATE_TIME_PATTERN.length
            val date = if (offset >= 0) {
              dateIndex.substring(offset)
            } else {
              dateIndex
            }
            ApiMetrics(date, hit.sourceAsMap.getOrElse(FieldKeys.FIELD_METRICS, Map.empty))
          }).sortWith((d1, d2) => d1.date < d2.date)
        } else {
          Nil
        }
      })
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  case class ApiMetrics(date: String, metrics: Any)

}
