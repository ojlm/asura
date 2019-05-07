package asura.core.es.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.model.{AggsItem, AggsQuery, QueryDomain, QueryDomainWildcard}
import asura.core.es.model.{BulkDocResponse, DomainOnlineLog, FieldKeys}
import asura.core.es.service.BaseAggregationService._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}

import scala.collection.mutable
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
    val yesterday = LocalDate.now().minusDays(1)
    val dates = if (StringUtils.isNotEmpty(query.date)) {
      Seq(query.date)
    } else {
      val datesSet = mutable.HashSet[String]()
      EsClient.esOnlineLogClients.foreach(config => {
        datesSet += yesterday.format(DateTimeFormatter.ofPattern(config.datePattern))
      })
      datesSet
    }
    if (dates.nonEmpty && StringUtils.isNotEmpty(query.domain)) {
      val subBoolQuery = boolQuery().should(
        prefixQuery(FieldKeys.FIELD_NAME, query.domain).boost(2D),
        wildcardQuery(FieldKeys.FIELD_NAME, s"*${query.domain}*").boost(1D)
      )
      val esQueries = if (StringUtils.isNotEmpty(query.tag)) {
        Seq(termsQuery(FieldKeys.FIELD_DATE, dates), termQuery(FieldKeys.FIELD_TAG, query.tag), subBoolQuery)
      } else {
        Seq(termsQuery(FieldKeys.FIELD_DATE, dates), subBoolQuery)
      }
      EsClient.esClient.execute {
        search(DomainOnlineLog.Index)
          .query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
      }
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  def queryDomain(query: QueryDomain) = {
    val esQueries = ArrayBuffer[Query]()
    var sortField = if (StringUtils.isNotEmpty(query.sortField)) {
      query.sortField
    } else {
      FieldKeys.FIELD_COUNT
    }
    val order: SortOrder = SortOrder.DESC
    if (StringUtils.isNotEmpty(query.date)) esQueries += termQuery(FieldKeys.FIELD_DATE, query.date)
    if (null != query.tag) {
      esQueries += termQuery(FieldKeys.FIELD_TAG, query.tag)
    } else {
      esQueries += termQuery(FieldKeys.FIELD_TAG, StringUtils.EMPTY)
    }
    if (null != query.names && query.names.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_NAME, query.names)
      if (query.names.length == 1) {
        sortField = FieldKeys.FIELD_DATE
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
