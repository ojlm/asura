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
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

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
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.domain)) esQueries += termQuery(FieldKeys.FIELD_DOMAIN, query.domain)
      if (StringUtils.isNotEmpty(query.method)) esQueries += termQuery(FieldKeys.FIELD_METHOD, query.method)
      if (StringUtils.isNotEmpty(query.urlPath)) esQueries += wildcardQuery(FieldKeys.FIELD_URL_PATH, s"${query.urlPath}*")
      EsClient.esClient.execute {
        search(s"${RestApiOnlineLog.Index}-${query.date}").query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
          .sortByFieldDesc(FieldKeys.FIELD_COUNT)
      }
    }
  }
}
