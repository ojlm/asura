package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.{AggsItem, AggsQuery, QueryDomain}
import asura.core.es.model.{BulkDocResponse, DomainOnlineLog, FieldKeys}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

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

  def syncOnlineDomain(domainCount: Int): Future[Seq[DomainOnlineLog]] = {
    var docs: Seq[DomainOnlineLog] = Nil
    OnlineRequestLogService.getOnlineDomain(domainCount).flatMap(items => {
      if (items.nonEmpty) {
        docs = items.map(item => DomainOnlineLog(item.id, item.count, item.`type`))
        index(docs).map(_ => docs)
      } else {
        Future.successful(Nil)
      }
    })
  }

  def aggTerms(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, false)
    val aggField = aggs.termsField
    EsClient.esClient.execute {
      search(DomainOnlineLog.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }

  def queryDomain(query: QueryDomain) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.date)) esQueries += termQuery(FieldKeys.FIELD_DATE, query.date)
    if (null != query.names && query.names.nonEmpty) esQueries += termsQuery(FieldKeys.FIELD_NAME, query.names)
    EsClient.esClient.execute {
      search(DomainOnlineLog.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_COUNT)
    }
  }
}
