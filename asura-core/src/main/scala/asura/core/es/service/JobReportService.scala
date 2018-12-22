package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.{AggsItem, AggsQuery, QueryJobReport}
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.DateHistogramInterval
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobReportService extends CommonService with BaseAggregationService {

  val queryIncludeFields = Seq(
    FieldKeys.FIELD_JOB_ID,
    FieldKeys.FIELD_JOB_NAME,
    FieldKeys.FIELD_TYPE,
    FieldKeys.FIELD_START_AT,
    FieldKeys.FIELD_END_AT,
    FieldKeys.FIELD_ELAPSE,
    FieldKeys.FIELD_RESULT,
    FieldKeys.FIELD_ERROR_MSG,
  )

  def index(report: JobReport): Future[IndexDocResponse] = {
    if (null == report) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        indexInto(JobReport.Index / EsConfig.DefaultType).doc(report).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(JobReport.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(JobReport.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(JobReport.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  // reindex report with job data
  def indexReport(id: String, jobReport: JobReport): Future[IndexDocResponse] = {
    if (null == jobReport) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      if (null == jobReport.statis && null != jobReport.data) {
        jobReport.statis = JobReportDataStatistic(jobReport.data)
      }
      EsClient.esClient.execute {
        indexInto(JobReport.Index / EsConfig.DefaultType).doc(jobReport).id(id)
      }.map(toIndexDocResponse(_))
    }
  }

  def query(report: QueryJobReport) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(report.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, report.group)
    if (StringUtils.isNotEmpty(report.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, report.project)
    if (StringUtils.isNotEmpty(report.classAlias)) esQueries += termQuery(FieldKeys.FIELD_CLASS_ALIAS, report.classAlias)
    if (StringUtils.isNotEmpty(report.scheduler)) esQueries += termQuery(FieldKeys.FIELD_SCHEDULER, report.scheduler)
    if (StringUtils.isNotEmpty(report.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, report.text)
    if (StringUtils.isNotEmpty(report.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, report.`type`)
    EsClient.esClient.execute {
      search(JobReport.Index)
        .query(boolQuery().must(esQueries))
        .from(report.pageFrom)
        .size(report.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(queryIncludeFields)
    }
  }

  def jobTrend(jobId: String, size: Int) = {
    EsClient.esClient.execute {
      search(JobReport.Index)
        .query(boolQuery().must(termQuery(FieldKeys.FIELD_JOB_ID, jobId)))
        .size(size)
        .sortByFieldDesc(FieldKeys.FIELD_END_AT)
        .sourceInclude(FieldKeys.FIELD_ELAPSE, FieldKeys.FIELD_STATIS, FieldKeys.FIELD_END_AT)
    }
  }

  def trend(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, false)
    val termsField = aggs.aggTermsField()
    val dateHistogram = dateHistogramAgg(aggsTermsName, FieldKeys.FIELD_CREATED_AT)
      .interval(DateHistogramInterval.fromString(aggs.aggInterval()))
      .format("yyyy-MM-dd")
      .subAggregations(termsAgg(aggsTermsName, termsField).size(aggs.pageSize()))
    EsClient.esClient.execute {
      search(JobReport.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, termsField))
  }
}
