package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.QueryCiEvents
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object TriggerEventLogService extends CommonService with BaseAggregationService {

  def index(items: Seq[TriggerEventLog]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(TriggerEventLog.Index / EsConfig.DefaultType).doc(item))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def queryEvents(query: QueryCiEvents) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.env)) esQueries += termQuery(FieldKeys.FIELD_ENV, query.env)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    if (StringUtils.isNotEmpty(query.service)) esQueries += termQuery(FieldKeys.FIELD_SERVICE, query.service)
    EsClient.esClient.execute {
      search(TriggerEventLog.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_TIMESTAMP)
    }
  }
}
