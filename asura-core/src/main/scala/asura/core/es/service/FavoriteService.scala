package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.QueryFavorite
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object FavoriteService extends CommonService with BaseAggregationService {

  def index(item: Favorite): Future[IndexDocResponse] = {
    if (null == item && StringUtils.hasEmpty(item.group, item.project, item.user,
      item.`type`, item.targetId, item.targetType, item.summary)
    ) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        indexInto(Favorite.Index / EsConfig.DefaultType)
          .doc(item)
          .id(item.generateDocId())
          .refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Favorite.Index / EsConfig.DefaultType)
      }.map(toDeleteDocResponse(_))
    }
  }

  def existDoc(id: String): Future[Boolean] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        exists(id, Favorite.Index, EsConfig.DefaultType)
      }.map(res => res.result)
    }
  }

  def queryFavorite(query: QueryFavorite) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.`type`)
    if (StringUtils.isNotEmpty(query.user)) esQueries += termQuery(FieldKeys.FIELD_USER, query.user)
    if (StringUtils.isNotEmpty(query.targetType)) esQueries += termQuery(FieldKeys.FIELD_TARGET_TYPE, query.targetType)
    if (StringUtils.isNotEmpty(query.targetId)) esQueries += termQuery(FieldKeys.FIELD_TARGET_ID, query.targetId)
    EsClient.esClient.execute {
      search(Favorite.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_TIMESTAMP)
    }
  }

}
