package asura.core.es.service

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model.{Activity, FieldKeys, JobReport}
import asura.core.model.AggsItem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.DateHistogramInterval

import scala.concurrent.Future

object CountService extends CommonService with BaseAggregationService {

  def dateHistogram(index: String): Future[Seq[AggsItem]] = {
    val dateHistogram = dateHistogramAgg(BaseAggregationService.aggsTermsName, FieldKeys.FIELD_CREATED_AT)
      .fixedInterval(DateHistogramInterval.fromString("1M")) // one month
      .format("yyyy-MM-dd")
    EsClient.esClient.execute {
      search(index)
        .query(matchAllQuery())
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, null))
  }

  def activityDateHistogram(`type`: String): Future[Seq[AggsItem]] = {
    val dateHistogram = dateHistogramAgg(BaseAggregationService.aggsTermsName, FieldKeys.FIELD_TIMESTAMP)
      .fixedInterval(DateHistogramInterval.fromString("1M")) // one month
      .format("yyyy-MM-dd")
    EsClient.esClient.execute {
      search(Activity.Index)
        .query(termQuery(FieldKeys.FIELD_TYPE, `type`))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, null))
  }

  def jobDateHistogram(`type`: String): Future[Seq[AggsItem]] = {
    val dateHistogram = dateHistogramAgg(BaseAggregationService.aggsTermsName, FieldKeys.FIELD_CREATED_AT)
      .fixedInterval(DateHistogramInterval.fromString("1M")) // one month
      .format("yyyy-MM-dd")
    EsClient.esClient.execute {
      search(JobReport.Index)
        .query(termQuery(FieldKeys.FIELD_TYPE, `type`))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, null))
  }

  def countIndex(index: String): Future[Long] = {
    EsClient.esClient.execute {
      count(index)
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countActivity(`type`: String): Future[Long] = {
    EsClient.esClient.execute {
      count(Activity.Index).query(termQuery(FieldKeys.FIELD_TYPE, `type`))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countActivities(types: Seq[String]): Future[Long] = {
    EsClient.esClient.execute {
      count(Activity.Index).query(termsQuery(FieldKeys.FIELD_TYPE, types))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }

  def countJob(`type`: String): Future[Long] = {
    EsClient.esClient.execute {
      count(JobReport.Index).query(termQuery(FieldKeys.FIELD_TYPE, `type`))
    }.map(res => {
      if (res.isSuccess) res.result.count else 0L
    })
  }
}
