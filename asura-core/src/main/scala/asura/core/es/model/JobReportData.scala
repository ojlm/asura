package asura.core.es.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.assertion.engine.Statistic
import asura.core.es.model.JobReportData.{JobReportItem, ScenarioReportItem}
import asura.core.http.HttpResult
import com.fasterxml.jackson.annotation.JsonIgnore


/**
  * Be careful to modify this class's schema, it should be compatible with data structure in ES.
  */
case class JobReportData(
                          var dayIndexSuffix: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern(JobReportDataItem.INDEX_DATE_TIME_PATTERN)),
                          var cases: Seq[JobReportItem] = Nil,
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
    var `type`: String = ScenarioStep.TYPE_HTTP
    // not empty when error occur
    var msg: String = StringUtils.EMPTY

    def markFail(): BasicReportItem = {
      this.status = ReportItemStatus.STATUS_FAIL
      this
    }

    @JsonIgnore
    def isSuccessful(): Boolean = {
      status == ReportItemStatus.STATUS_PASS
    }

    @JsonIgnore
    def isSkipped(): Boolean = {
      status == ReportItemStatus.STATUS_SKIPPED
    }

    @JsonIgnore
    def isFailed(): Boolean = {
      status == ReportItemStatus.STATUS_FAIL
    }
  }

  case class JobReportItemMetrics(
                                   val renderRequestTime: Long,
                                   val renderAuthTime: Long,
                                   val requestTime: Long,
                                   val evalAssertionTime: Long,
                                   val totalTime: Long,
                                 )

  /**
    * for any type of request. eg: http, dubbo, sql
    */
  case class JobReportItem(
                            var id: String,
                            var title: String,
                            var itemId: String,
                            var statis: Statistic,
                            var generator: String = StringUtils.EMPTY, // specify generator type
                          ) extends BasicReportItem

  object JobReportItem {

    def parse(title: String, result: HttpResult, itemId: String = null, status: String = null, msg: String = null): JobReportItem = {
      val item = JobReportItem(id = result.caseId, title = title, itemId, statis = result.statis)
      if (StringUtils.isNotEmpty(status)) {
        item.status = status
      } else {
        item.status = if (result.statis.isSuccessful) ReportItemStatus.STATUS_PASS else ReportItemStatus.STATUS_FAIL
      }
      if (StringUtils.isNotEmpty(result.generator)) item.generator = result.generator
      if (null != msg) item.msg = msg
      item
    }
  }

  /**
    * @param id    scenarioId
    * @param title summary
    * @param steps CaseReportItem array
    */
  case class ScenarioReportItem(
                                 var id: String,
                                 var title: String,
                                 var steps: Seq[JobReportItem] = Nil
                               ) extends BasicReportItem

}
