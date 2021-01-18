package asura.core.es.service

import asura.common.util.{DateUtils, JsonUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.JobExecDesc
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.ui.driver.{CommandMeta, DriverCommandEnd}
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.mutable
import scala.concurrent.Future

object UiTaskReportService extends CommonService {

  def index(report: UiTaskReport): Future[IndexDocResponse] = {
    EsClient.esClient.execute {
      indexInto(UiTaskReport.Index / EsConfig.DefaultType).doc(report).refresh(RefreshPolicy.WaitFor)
    }.map(toIndexDocResponse(_))
  }

  def updateReport(meta: CommandMeta, data: DriverCommandEnd) = {
    val m = mutable.Map[String, Any]()
    m += (FieldKeys.FIELD_END_AT -> meta.endAt)
    m += (FieldKeys.FIELD_ELAPSE -> (meta.endAt - meta.startAt))
    if (!data.ok) {
      m += (FieldKeys.FIELD_RESULT -> JobExecDesc.STATUS_FAIL)
      m += (FieldKeys.FIELD_ERROR_MSG -> data.msg)
    }
    m += (FieldKeys.FIELD_DATA -> UiTaskReportData(data.result))
    m += (FieldKeys.FIELD_UPDATED_AT -> DateUtils.nowDateTime)
    EsClient.esClient.execute {
      update(meta.reportId).in(UiTaskReport.Index / EsConfig.DefaultType).doc(JsonUtils.stringify(m))
    }.map(toUpdateDocResponse(_))
  }

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(UiTaskReport.Index).query(idsQuery(id)).size(1)
    }
  }

}
