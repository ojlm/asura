package asura.core.es.model

import asura.core.cs.CaseResult
import asura.core.es.model.JobReportData.{CaseReportItem, ScenarioReportItem}


/**
  * Be careful to modify this class's schema, it should be compatible with data structure in ES.
  */
case class JobReportData(
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

  case class CaseReportItem(
                             var id: String,
                             var title: String,
                             var result: CaseResult = null
                           ) extends BasicReportItem

  object CaseReportItem {
    def parse(title: String, result: CaseResult, msg: String = null): CaseReportItem = {
      val item = CaseReportItem(id = result.id, title = title, result = result)
      item.status = if (result.statis.isSuccessful) ReportItemStatus.STATUS_SUCCESS else ReportItemStatus.STATUS_FAIL
      item.msg = if (null != msg) msg else item.status
      item
    }
  }

  case class ScenarioReportItem(
                                 var id: String,
                                 var title: String,
                                 var cases: Seq[CaseReportItem] = Nil
                               ) extends BasicReportItem

}
