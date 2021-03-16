package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.actor.UiTaskListenerActor.WrappedLog
import asura.core.es.model.{BulkDocResponse, FieldKeys, LogEntry}
import asura.core.es.service.CommonService.CustomCatIndices
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.SearchAfterLogEntry
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.TermBucket
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object LogEntryService extends CommonService {

  def index(items: Seq[WrappedLog]): Future[BulkDocResponse] = {
    EsClient.esClient.execute {
      bulk(
        items.map(item => indexInto(s"${LogEntry.Index}-${item.date}" / EsConfig.DefaultType).doc(item.log))
      )
    }.map(toBulkDocResponse(_))
  }

  def getIndices() = {
    EsClient.esClient.execute {
      CustomCatIndices(s"${LogEntry.Index}-*")
    }
  }

  def getAggs(query: SearchAfterLogEntry) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.reportId)) esQueries += termQuery(FieldKeys.FIELD_REPORT_ID, query.reportId)
    EsClient.esClient.execute {
      search(s"${LogEntry.Index}-${query.day}")
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(
          termsAgg(FieldKeys.FIELD_HOSTNAME, FieldKeys.FIELD_HOSTNAME),
          termsAgg(FieldKeys.FIELD_TYPE, FieldKeys.FIELD_TYPE).subaggs(
            termsAgg(FieldKeys.FIELD_SOURCE, FieldKeys.FIELD_SOURCE),
            termsAgg(FieldKeys.FIELD_METHOD, FieldKeys.FIELD_METHOD),
          ),
        )
    }.map(res => {
      if (res.isSuccess) {
        val aggregations = res.result.aggregations
        Map(
          FieldKeys.FIELD_HOSTNAME -> aggregations.terms(FieldKeys.FIELD_HOSTNAME).buckets
            .map(bucket => Map("name" -> bucket.key, "value" -> bucket.docCount)),
          FieldKeys.FIELD_TYPE -> aggregations.terms(FieldKeys.FIELD_TYPE).buckets
            .map(bucket => Map("name" -> bucket.key, "value" -> bucket.docCount, "extra" -> getSourceAndMethod(bucket))),
        )
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

  def getSourceAndMethod(bucket: TermBucket): Map[String, Any] = {
    val data = bucket.dataAsMap
    if (data != null) {
      val sourceOpt = data.get(FieldKeys.FIELD_SOURCE)
      val methodOpt = data.get(FieldKeys.FIELD_METHOD)
      var source: List[Map[String, Any]] = null
      var method: List[Map[String, Any]] = null
      if (sourceOpt.nonEmpty) {
        val buckets = sourceOpt.get.asInstanceOf[Map[String, Any]].get("buckets")
        if (buckets != null) {
          source = buckets.get.asInstanceOf[List[Map[String, Any]]]
            .map(item => Map("name" -> item.get("key").get, "value" -> item.get("doc_count")))
        }
      }
      if (methodOpt.nonEmpty) {
        val buckets = methodOpt.get.asInstanceOf[Map[String, Any]].get("buckets")
        if (buckets != null) {
          method = buckets.get.asInstanceOf[List[Map[String, Any]]]
            .map(item => Map("name" -> item.get("key").get, "value" -> item.get("doc_count")))
        }
      }
      Map("source" -> source, "method" -> method)
    } else {
      Map.empty
    }
  }

  def searchFeed(query: SearchAfterLogEntry) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.reportId)) esQueries += termQuery(FieldKeys.FIELD_REPORT_ID, query.reportId)
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD_TEXT, query.text)
    if (query.`type` != null && query.`type`.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_TYPE, query.`type`)
    }
    if (query.source != null && query.source.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_SOURCE, query.source)
    }
    if (query.hostname != null && query.hostname.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_HOSTNAME, query.hostname)
    }
    if (query.method != null && query.method.nonEmpty) {
      esQueries += termsQuery(FieldKeys.FIELD_METHOD, query.method)
    }
    if (query.levels != null && query.levels.nonEmpty) {
      esQueries += boolQuery().should(query.levels.map(level => {
        if (level.equals("unknown")) {
          not(existsQuery(FieldKeys.FIELD_LEVEL))
        } else {
          termQuery(FieldKeys.FIELD_LEVEL, level)
        }
      }))
    }
    EsClient.esClient.execute {
      search(s"${LogEntry.Index}-${query.day}")
        .query(boolQuery().must(esQueries))
        .size(query.pageSize)
        .searchAfter(query.toSearchAfterSort)
        .sortBy(if (query.desc) FieldSort(FieldKeys.FIELD_TIMESTAMP).desc() else FieldSort(FieldKeys.FIELD_TIMESTAMP).asc())
    }.map(res => {
      if (res.isSuccess) {
        val hits = res.result.hits
        Map(
          "total" -> hits.total,
          "list" -> hits.hits.map(_.sourceAsMap),
          "sort" -> (if (hits.hits.nonEmpty) hits.hits.last.sort.getOrElse(Nil) else Nil)
        )
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

}
