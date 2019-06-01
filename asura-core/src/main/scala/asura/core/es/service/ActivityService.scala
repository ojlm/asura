package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.{AggsItem, AggsQuery, QueryActivity}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.script.Script
import com.sksamuel.elastic4s.searches.DateHistogramInterval
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ActivityService extends CommonService with BaseAggregationService {

  val recentProjectScript = s"doc['${FieldKeys.FIELD_GROUP}'].value + '/' + doc['${FieldKeys.FIELD_PROJECT}'].value"

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
      search(HttpCaseRequest.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_TIMESTAMP)
    }
  }

  def trend(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, true)
    val termsField = aggs.aggTermsField()
    val dateHistogram = dateHistogramAgg(aggsTermsName, FieldKeys.FIELD_TIMESTAMP)
      .interval(DateHistogramInterval.fromString(aggs.aggInterval()))
      .format("yyyy-MM-dd")
      .subAggregations(termsAgg(aggsTermsName, if (termsField.equals("creator")) FieldKeys.FIELD_USER else termsField).size(aggs.pageSize()))
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, termsField))
  }

  def aggTerms(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, true)
    val aggField = aggs.aggField()
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermsName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }

  def recentProjects(user: String, me: Boolean = true, wd: String = null, size: Int = 20, excludeGPs: Seq[(String, String)] = Nil): Future[Seq[AggsItem]] = {
    val esQueries = ArrayBuffer[Query]()
    if (me) {
      esQueries += termQuery(FieldKeys.FIELD_USER, user)
    } else {
      esQueries += not(termQuery(FieldKeys.FIELD_USER, user))
      // from a month ago
      esQueries += rangeQuery(FieldKeys.FIELD_TIMESTAMP).gte("now-30d")
    }
    esQueries += not(termsQuery(FieldKeys.FIELD_TYPE, Seq(Activity.TYPE_NEW_USER, Activity.TYPE_USER_LOGIN)))
    if (excludeGPs.nonEmpty) {
      esQueries += not(should(excludeGPs.map(gp =>
        must(termQuery(FieldKeys.FIELD_GROUP, gp._1), termQuery(FieldKeys.FIELD_PROJECT, gp._2)))
      ))
    }
    if (StringUtils.isNotEmpty(wd)) {
      esQueries += should(
        wildcardQuery(FieldKeys.FIELD_GROUP, s"*${wd}*"),
        wildcardQuery(FieldKeys.FIELD_PROJECT, s"*${wd}*")
      )
    }
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(
          termsAggregation(aggsTermsName)
            .script(Script(recentProjectScript))
            .size(size)
        )
    }.map(toAggItems(_, null, null))
  }
}
