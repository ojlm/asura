package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.{AggsItem, AggsQuery, QueryActivity}
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.DateHistogramInterval
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ActivityService extends CommonService {

  def index(items: Seq[Activity]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(Activity.Index / EsConfig.DefaultType).doc(item))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def queryActivity(query: QueryActivity) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.`type`)
    if (StringUtils.isNotEmpty(query.user)) esQueries += termQuery(FieldKeys.FIELD_USER, query.user)
    if (StringUtils.isNotEmpty(query.targetId)) esQueries += termQuery(FieldKeys.FIELD_TARGET_ID, query.targetId)
    EsClient.esClient.execute {
      search(Case.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_TIMESTAMP)
    }
  }

  def trend(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(aggs.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, aggs.group)
    if (StringUtils.isNotEmpty(aggs.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, aggs.project)
    if (StringUtils.isNotEmpty(aggs.creator)) esQueries += termQuery(FieldKeys.FIELD_USER, aggs.creator)
    if (null != aggs.types && aggs.types.nonEmpty) esQueries += termsQuery(FieldKeys.FIELD_TYPE, aggs.types)
    if (StringUtils.isNotEmpty(aggs.dateRange)) esQueries += rangeQuery(FieldKeys.FIELD_TIMESTAMP).gte(s"now-${aggs.dateRange}/d").lte(s"now/d")
    val termsField = aggs.aggTermsField()
    val dateHistogram = dateHistogramAgg(aggsTermName, FieldKeys.FIELD_TIMESTAMP)
      .interval(DateHistogramInterval.fromString(aggs.aggInterval()))
      .format("yyyy-MM-dd")
      .subAggregations(termsAgg(aggsTermName, if (termsField.equals("creator")) FieldKeys.FIELD_USER else termsField).size(aggs.pageSize()))
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(dateHistogram)
    }.map(res => {
      val buckets = res.result
        .aggregationsAsMap.getOrElse(aggsTermName, Map.empty)
        .asInstanceOf[Map[String, Any]]
        .getOrElse("buckets", Nil)
      buckets.asInstanceOf[Seq[Map[String, Any]]].map(bucket => {
        AggsItem(
          `type` = null,
          id = bucket.getOrElse("key_as_string", "").asInstanceOf[String],
          count = bucket.getOrElse("doc_count", 0).asInstanceOf[Int],
          sub = {
            bucket.getOrElse(aggsTermName, Map.empty)
              .asInstanceOf[Map[String, Any]]
              .getOrElse("buckets", Nil)
              .asInstanceOf[Seq[Map[String, Any]]].map(bucket => {
              AggsItem(
                `type` = termsField,
                id = bucket.getOrElse("key", "").asInstanceOf[String],
                count = bucket.getOrElse("doc_count", 0).asInstanceOf[Int]
              )
            })
          })
      })
    })
  }
}
