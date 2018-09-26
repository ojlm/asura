package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryJobReport
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobReportService extends CommonService {

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


  def updateReport(id: String, jobReport: JobReport): Future[UpdateDocResponse] = {
    if (null == jobReport && jobReport.toUpdateMap.nonEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        update(id).in(JobReport.Index / EsConfig.DefaultType).doc(jobReport.toUpdateMap)
      }.map(toUpdateDocResponse(_))
    }
  }

  def query(report: QueryJobReport) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(report.group)) {
      esQueries += termQuery(FieldKeys.FIELD_GROUP, report.group)
    }
    if (StringUtils.isNotEmpty(report.classAlias)) {
      esQueries += termQuery(FieldKeys.FIELD_CLASS_ALIAS, report.classAlias)
    }
    if (StringUtils.isNotEmpty(report.scheduler)) {
      esQueries += termQuery(FieldKeys.FIELD_SCHEDULER, report.scheduler)
    }
    if (StringUtils.isNotEmpty(report.`type`)) {
      esQueries += termQuery(FieldKeys.FIELD_TYPE, report.`type`)
    }
    EsClient.esClient.execute {
      val clause = search(JobReport.Index).query {
        boolQuery().must(esQueries)
      }
      clause.sourceExclude(FieldKeys.FIELD_DATA)
        .from(report.pageFrom)
        .size(report.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }
}
