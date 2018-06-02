package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryJobReport
import asura.core.es.model.{FieldKeys, JobReport}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer

object JobReportService {

  def index(report: JobReport) = {
    if (null == report) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        indexInto(JobReport.Index / EsConfig.DefaultType).doc(report).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(JobReport.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        bulk(ids.map(id => delete(id).from(JobReport.Index)))
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(JobReport.Index).query(idsQuery(id))
      }
    }
  }


  def updateReport(id: String, jobReport: JobReport) = {
    if (null == jobReport && jobReport.toUpdateMap.nonEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        update(id).in(JobReport.Index / EsConfig.DefaultType).doc(jobReport.toUpdateMap)
      }
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
      clause.from(report.pageFrom).size(report.pageSize).sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }
}
