package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.cs.model.{AggsItem, AggsQuery}
import asura.core.es.model.FieldKeys
import asura.core.es.service.ActivityService.aggsTermName
import com.sksamuel.elastic4s.http.ElasticDsl.{rangeQuery, termQuery, termsQuery, wildcardQuery, _}
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer

trait BaseAggregationService {

  def toAggItems(res: Response[SearchResponse], termsField: String): Seq[AggsItem] = {
    val buckets = res.result
      .aggregationsAsMap.getOrElse(aggsTermName, Map.empty)
      .asInstanceOf[Map[String, Any]]
      .getOrElse("buckets", Nil)
    buckets.asInstanceOf[Seq[Map[String, Any]]].map(bucket => {
      AggsItem(
        `type` = null,
        id = bucket.getOrElse("key_as_string", "").asInstanceOf[String],
        count = bucket.getOrElse("doc_count", 0).asInstanceOf[Int],
        sub =
          if (StringUtils.isEmpty(termsField)) {
            null
          } else {
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
    esQueries
  }
}
