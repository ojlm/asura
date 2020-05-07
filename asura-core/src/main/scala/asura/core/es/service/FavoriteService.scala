package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.model.{AggsItem, AggsQuery, QueryFavorite}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object FavoriteService extends CommonService with BaseAggregationService {

  def getById(id: String): Future[Favorite] = {
    EsClient.esClient.execute {
      search(Favorite.Index).query(idsQuery(id)).size(1)
    }.map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        val hit = res.result.hits.hits(0)
        JacksonSupport.parse(hit.sourceAsString, classOf[Favorite])
      } else {
        throw ErrorMessages.error_EmptyId.toException
      }
    })
  }

  def index(item: Favorite): Future[IndexDocResponse] = {
    val error = validate(item)
    if (null != error) {
      error.toFutureFail
    } else {
      item.id = item.generateLogicId()
      EsClient.esClient.execute {
        indexInto(Favorite.Index)
          .doc(item)
          .refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    }
  }

  def validate(item: Favorite): ErrorMessage = {
    if (null == item || StringUtils.hasEmpty(item.group, item.project, item.user,
      item.`type`, item.targetId, item.targetType, item.summary)) {
      ErrorMessages.error_InvalidParams
    } else {
      null
    }
  }

  def getByLogicId(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Favorite.Index)
          .query(boolQuery().must(termQuery(FieldKeys.FIELD_ID, id)))
          .size(1)
      }
    }
  }

  def check(docId: String, value: Boolean, summary: String = null) = {
    EsClient.esClient.execute {
      val m = if (value) {
        Map(FieldKeys.FIELD_CHECKED -> value, FieldKeys.FIELD_SUMMARY -> summary)
      } else {
        Map(FieldKeys.FIELD_CHECKED -> value)
      }
      update(docId).in(Favorite.Index).doc(m)
    }.map(toUpdateDocResponse(_))
  }

  def queryFavorite(query: QueryFavorite) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    if (StringUtils.isNotEmpty(query.user)) esQueries += termQuery(FieldKeys.FIELD_USER, query.user)
    if (StringUtils.isNotEmpty(query.targetType)) esQueries += termQuery(FieldKeys.FIELD_TARGET_TYPE, query.targetType)
    if (StringUtils.isNotEmpty(query.targetId)) esQueries += termQuery(FieldKeys.FIELD_TARGET_ID, query.targetId)
    if (StringUtils.isNotEmpty(query.checked)) {
      query.checked match {
        case "true" => esQueries += termQuery(FieldKeys.FIELD_CHECKED, true)
        case "false" => esQueries += termQuery(FieldKeys.FIELD_CHECKED, false)
        case _ =>
      }
    }
    EsClient.esClient.execute {
      search(Favorite.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_TIMESTAMP)
    }
  }

  def termsAggs(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, false)
    val aggField = aggs.aggField()
    EsClient.esClient.execute {
      search(Favorite.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermsName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }
}
