package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryActivity
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
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
}
