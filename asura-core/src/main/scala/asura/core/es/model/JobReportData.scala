package asura.core.es.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
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

    val STATUS_PASS = "pass"
    val STATUS_FAIL = "fail"
    val STATUS_SKIPPED = "skipped" // item skipped
  }

  trait BasicReportItem {
    var id: String
    var title: String
    var status: String = ReportItemStatus.STATUS_PASS
    var msg: String = ReportItemStatus.STATUS_PASS

    def markFail(msg: String = ReportItemStatus.STATUS_FAIL): BasicReportItem = {
      this.status = ReportItemStatus.STATUS_FAIL
      this.msg = msg
      this
    }

    def isSuccessful(): Boolean = {
      status == ReportItemStatus.STATUS_PASS
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

    def parse(title: String, result: CaseResult, status: String = null, msg: String = null): CaseReportItem = {
      val item = CaseReportItem(id = result.caseId, title = title, statis = result.statis)
      if (StringUtils.isNotEmpty(status)) {
        item.status = status
      } else {
        item.status = if (result.statis.isSuccessful) ReportItemStatus.STATUS_PASS else ReportItemStatus.STATUS_FAIL
      }
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
