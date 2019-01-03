package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.{AggsItem, AggsQuery, QueryDomain, QueryDomainWildcard}
import asura.core.es.model.{BulkDocResponse, DomainOnlineLog, FieldKeys}
import asura.core.es.service.BaseAggregationService._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object DomainOnlineLogService extends CommonService with BaseAggregationService {

  def index(items: Seq[DomainOnlineLog]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(DomainOnlineLog.Index / EsConfig.DefaultType).doc(item).id(item.generateDocId()))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def aggTerms(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, false)
    val aggField = aggs.termsField
    EsClient.esClient.execute {
      search(DomainOnlineLog.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermsName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }

  def queryDomainWildcard(query: QueryDomainWildcard) = {
    if (StringUtils.isNotEmpty(query.date) && StringUtils.isNotEmpty(query.domain)) {
      EsClient.esClient.execute {
        search(DomainOnlineLog.Index)
          .query(boolQuery().must(
            termQuery(FieldKeys.FIELD_DATE, query.date),
            boolQuery().should(
              prefixQuery(FieldKeys.FIELD_NAME, query.domain).boost(2D),
              wildcardQuery(FieldKeys.FIELD_NAME, s"*${query.domain}*").boost(1D)
            )
          ))
          .from(query.pageFrom)
          .size(query.pageSize)
      }
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  def queryDomain(query: QueryDomain) = {
    val esQueries = ArrayBuffer[Query]()
    var sortField = query.sortField
    var order: SortOrder = SortOrder.DESC
    if (StringUtils.isNotEmpty(query.date)) esQueries += termQuery(FieldKeys.FIELD_DATE, query.date)
    if (StringUtils.isNotEmpty(query.tag)) esQueries += termQuery(FieldKeys.FIELD_TAG, query.tag)
    if (null != query.names && query.names.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_NAME, query.names)
      if (query.names.length == 1) {
        sortField = FieldKeys.FIELD_DATE
        order = SortOrder.ASC
      }
    }
    EsClient.esClient.execute {
      search(DomainOnlineLog.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(FieldSort(field = sortField, order = order))
    }
  }
}
