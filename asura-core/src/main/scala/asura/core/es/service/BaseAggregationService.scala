package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.cs.model.{AggsItem, AggsQuery}
import asura.core.es.model.FieldKeys
import com.sksamuel.elastic4s.http.ElasticDsl.{rangeQuery, termQuery, termsQuery, wildcardQuery, _}
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer

trait BaseAggregationService {

  import BaseAggregationService._

  def toMetricsAggregation(field: String): Seq[AbstractAggregation] = {
    if (StringUtils.isNotEmpty(field)) {
      Seq(
        percentilesAgg(aggsPercentilesName, field).percents(percentilesPercents),
        avgAgg(aggsAvg, field),
        minAgg(aggsMin, field),
        maxAgg(aggsMax, field),
      )
    } else {
      Nil
    }
  }

  // assume the metrics aggregations are child aggregation of terms aggregation
  def toAggItems(res: Response[SearchResponse], itemType: String, subItemType: String): Seq[AggsItem] = {
    val buckets = res.result
      .aggregationsAsMap.getOrElse(aggsTermsName, Map.empty)
      .asInstanceOf[Map[String, Any]]
      .getOrElse("buckets", Nil)
    buckets.asInstanceOf[Seq[Map[String, Any]]].map(bucket => {
      AggsItem(
        `type` = itemType,
        // 'key_as_string' for date field.
        id = bucket.getOrElse("key_as_string", bucket.getOrElse("key", "")).asInstanceOf[String],
        count = bucket.getOrElse("doc_count", 0).toString.toLong,
        sub =
          if (StringUtils.isEmpty(subItemType)) {
            null
          } else {
            bucket.getOrElse(aggsTermsName, Map.empty)
              .asInstanceOf[Map[String, Any]]
              .getOrElse("buckets", Nil)
              .asInstanceOf[Seq[Map[String, Any]]].map(subBucket => {
              AggsItem(
                `type` = subItemType,
                id = subBucket.getOrElse("key_as_string", subBucket.getOrElse("key", "")).asInstanceOf[String],
                count = subBucket.getOrElse("doc_count", 0).toString.toLong
              ).evaluateBucketToMetrics(subBucket)
            })
          }).evaluateBucketToMetrics(bucket)
    })
  }

  def buildEsQueryFromAggQuery(aggs: AggsQuery, isActivity: Boolean = false): Seq[Query] = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(aggs.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, aggs.group)
    if (StringUtils.isNotEmpty(aggs.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, aggs.project)
    if (StringUtils.isNotEmpty(aggs.creator)) {
      if (isActivity) {
        esQueries += termQuery(FieldKeys.FIELD_USER, aggs.creator)
      } else {
        esQueries += termQuery(FieldKeys.FIELD_CREATOR, aggs.creator)
      }
    }
    if (StringUtils.isNotEmpty(aggs.creatorPrefix)) {
      if (isActivity) {
        esQueries += wildcardQuery(FieldKeys.FIELD_USER, s"${aggs.creatorPrefix}*")
      } else {
        esQueries += wildcardQuery(FieldKeys.FIELD_CREATOR, s"${aggs.creatorPrefix}*")
      }
    }
    if (null != aggs.types && aggs.types.nonEmpty) esQueries += termsQuery(FieldKeys.FIELD_TYPE, aggs.types)
    if (StringUtils.isNotEmpty(aggs.dateRange)) {
      if (isActivity) {
        esQueries += rangeQuery(FieldKeys.FIELD_TIMESTAMP).gte(s"now-${aggs.dateRange}/d").lte(s"now/d")
      } else {
        esQueries += rangeQuery(FieldKeys.FIELD_CREATED_AT).gte(s"now-${aggs.dateRange}/d").lte(s"now/d")
      }
    }
    if (StringUtils.isNotEmpty(aggs.namePrefix)) esQueries += wildcardQuery(FieldKeys.FIELD_NAME, s"${aggs.namePrefix}*")
    if (StringUtils.isNotEmpty(aggs.date)) esQueries += termQuery(FieldKeys.FIELD_DATE, aggs.date)
    esQueries
  }
}

object BaseAggregationService {
  val aggsTermsName = "terms"
  val aggsPercentilesName = "percentiles"
  val percentilesPercents = Array(25, 50, 75, 90, 95, 99, 99.9)
  val aggsAvg = "avg"
  val aggsMin = "min"
  val aggsMax = "max"
}
