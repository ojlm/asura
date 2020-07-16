package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model.FieldKeys
import asura.core.model.{AggsItem, AggsQuery}
import com.sksamuel.elastic4s.ElasticDsl.{rangeQuery, termQuery, termsQuery, wildcardQuery, _}
import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

trait BaseAggregationService {

  import BaseAggregationService._

  def aggsLabels(index: String, labelPrefix: String): Future[Seq[AggsItem]] = {
    val query = if (StringUtils.isNotEmpty(labelPrefix)) {
      nestedQuery(
        FieldKeys.FIELD_LABELS,
        wildcardQuery(FieldKeys.FIELD_NESTED_LABELS_NAME, s"${labelPrefix}*")
      )
    } else {
      matchAllQuery()
    }
    EsClient.esClient.execute {
      search(index)
        .query(query)
        .size(0)
        .aggregations(
          nestedAggregation(FieldKeys.FIELD_LABELS, FieldKeys.FIELD_LABELS)
            .subAggregations(termsAgg(FieldKeys.FIELD_LABELS, FieldKeys.FIELD_NESTED_LABELS_NAME))
        )
    }.map(res => {
      val buckets = res.result
        .aggregationsAsMap.getOrElse(FieldKeys.FIELD_LABELS, Map.empty).asInstanceOf[Map[String, Any]]
        .getOrElse(FieldKeys.FIELD_LABELS, Map.empty).asInstanceOf[Map[String, Any]]
        .getOrElse("buckets", Nil)
      buckets.asInstanceOf[Seq[Map[String, Any]]].map(bucket => {
        AggsItem(
          `type` = null,
          id = bucket.getOrElse("key", "").asInstanceOf[String],
          count = bucket.getOrElse("doc_count", 0).asInstanceOf[Int],
          summary = null,
          description = null
        )
      })
    })
  }

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
  def toAggItems(res: Response[SearchResponse], itemType: String, subItemType: String, resolution: Double = 1D): Seq[AggsItem] = {
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
              ).evaluateBucketToMetrics(subBucket, resolution)
            })
          }).evaluateBucketToMetrics(bucket, resolution)
    })
  }

  def buildEsQueryFromAggQuery(aggs: AggsQuery, isActivity: Boolean = false): collection.Seq[Query] = {
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
    if (StringUtils.isNotEmpty(aggs.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, aggs.`type`)
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
    if (StringUtils.isNotEmpty(aggs.checked)) {
      aggs.checked match {
        case "true" => esQueries += termQuery(FieldKeys.FIELD_CHECKED, true)
        case "false" => esQueries += termQuery(FieldKeys.FIELD_CHECKED, false)
        case _ =>
      }
    }
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
