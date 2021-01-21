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

  def searchFeed(query: SearchAfterLogEntry) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.reportId)) esQueries += termQuery(FieldKeys.FIELD_REPORT_ID, query.reportId)
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD_TEXT, query.text)
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
        .sortBy(FieldSort(FieldKeys.FIELD_TIMESTAMP).desc())
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
