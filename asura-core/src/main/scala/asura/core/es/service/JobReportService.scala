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
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobReportService extends CommonService {

  def index(report: JobReport): Future[IndexDocResponse] = {
    if (null == report) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      if (null != report.data) {
        val reportData = report.data
        if (null != reportData.cases) {
          reportData.cases.foreach(cs => cs.freeData())
        }
        if (null != reportData.scenarios) {
          reportData.scenarios.foreach(s => {
            if (null != s.cases) s.cases.foreach(cs => cs.freeData())
          })
        }
      }
      EsClient.httpClient.execute {
        indexInto(JobReport.Index / EsConfig.DefaultType).doc(report).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(JobReport.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        bulk(ids.map(id => delete(id).from(JobReport.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(JobReport.Index).query(idsQuery(id)).size(1)
      }
    }
  }


  def updateReport(id: String, jobReport: JobReport): Future[UpdateDocResponse] = {
    if (null == jobReport && jobReport.toUpdateMap.nonEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        update(id).in(JobReport.Index / EsConfig.DefaultType).doc(jobReport.toUpdateMap)
      }.map(toUpdateDocResponse(_))
    }
  }

  def query(report: QueryJobReport) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(report.group)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_GROUP, report.group)
    }
    if (StringUtils.isNotEmpty(report.classAlias)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_CLASS_ALIAS, report.classAlias)
    }
    if (StringUtils.isNotEmpty(report.scheduler)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_SCHEDULER, report.scheduler)
    }
    if (StringUtils.isNotEmpty(report.`type`)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_TYPE, report.`type`)
    }
    EsClient.httpClient.execute {
      val clause = search(JobReport.Index).query {
        boolQuery().must(queryDefinitions)
      }
      clause.sourceExclude(FieldKeys.FIELD_DATA)
        .from(report.pageFrom)
        .size(report.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }
}
