package asura.core.es.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import asura.core.cs.CaseResult
import asura.core.cs.assertion.engine.Statistic
import asura.core.es.model.JobReportData.{CaseReportItem, ScenarioReportItem}


/**
  * Be careful to modify this class's schema, it should be compatible with data structure in ES.
  */
case class JobReportData(
                          var dayIndexSuffix: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern(JobReportDataItem.INDEX_DATE_TIME_PATTERN)),
                          var cases: Seq[CaseReportItem] = Nil,
                          var scenarios: Seq[ScenarioReportItem] = Nil,
                          var ext: Map[String, Any] = Map.empty
                        )

object JobReportData {

  object ReportItemStatus {
    val STATUS_SUCCESS = "success"
    val STATUS_WARN = "warn"
    val STATUS_FAIL = "fail"
  }

  trait BasicReportItem {
    var id: String
    var title: String
    var status: String = ReportItemStatus.STATUS_SUCCESS
    var msg: String = ReportItemStatus.STATUS_SUCCESS

    def markFail(msg: String = ReportItemStatus.STATUS_FAIL): BasicReportItem = {
      this.title = msg
      this.status = ReportItemStatus.STATUS_FAIL
      this.msg = ReportItemStatus.STATUS_FAIL
      this
    }

    def isSuccessful(): Boolean = {
      status == ReportItemStatus.STATUS_SUCCESS
    }
  }

  case class CaseReportItemMetrics(
                                    val renderRequestTime: Long,
                                    val renderAuthTime: Long,
                                    val requestTime: Long,
                                    val evalAssertionTime: Long,
                                    val totalTime: Long,
                                  )

  /**
    * @param id    caseId
    * @param title summary
    */
  case class CaseReportItem(
                             var id: String,
                             var title: String,
                             var statis: Statistic,
                           ) extends BasicReportItem

  object CaseReportItem {
    def parse(title: String, result: CaseResult, msg: String = null): CaseReportItem = {
      val item = CaseReportItem(id = result.caseId, title = title, statis = result.statis)
      item.status = if (result.statis.isSuccessful) ReportItemStatus.STATUS_SUCCESS else ReportItemStatus.STATUS_FAIL
      item.msg = if (null != msg) msg else item.status
      item
    }
  }

  /**
    * @param id    scenarioId
    * @param title summary
    * @param cases CaseReportItem array
    */
  case class ScenarioReportItem(
                                 var id: String,
                                 var title: String,
                                 var cases: Seq[CaseReportItem] = Nil
                               ) extends BasicReportItem

}
