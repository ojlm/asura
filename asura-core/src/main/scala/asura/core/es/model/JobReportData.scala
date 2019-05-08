package asura.core.es.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.assertion.engine.Statistic
import asura.core.es.model.JobReportData.{JobReportStepItemData, ScenarioReportItemData}
import asura.core.http.HttpResult
import com.fasterxml.jackson.annotation.JsonIgnore


/**
  * Be careful to modify this class's schema, it should be compatible with data structure in ES.
  */
case class JobReportData(
                          var dayIndexSuffix: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern(JobReportDataItem.INDEX_DATE_TIME_PATTERN)),
                          var cases: Seq[JobReportStepItemData] = Nil,
                          var scenarios: Seq[ScenarioReportItemData] = Nil,
                          var ext: Map[String, Any] = Map.empty
                        )

object JobReportData {

  object ReportStepItemStatus {

    val STATUS_PASS = "pass"
    val STATUS_FAIL = "fail"
    val STATUS_SKIPPED = "skipped" // item skipped
  }

  trait BasicReportItemData {
    var id: String
    var title: String
    var status: String = ReportStepItemStatus.STATUS_PASS

    // not empty when error occur
    var msg: String = StringUtils.EMPTY

    def markFail(): BasicReportItemData = {
      this.status = ReportStepItemStatus.STATUS_FAIL
      this
    }

    @JsonIgnore
    def isSuccessful(): Boolean = {
      status == ReportStepItemStatus.STATUS_PASS
    }

    @JsonIgnore
    def isSkipped(): Boolean = {
      status == ReportStepItemStatus.STATUS_SKIPPED
    }

    @JsonIgnore
    def isFailed(): Boolean = {
      status == ReportStepItemStatus.STATUS_FAIL
    }
  }

  case class JobReportStepItemMetrics(
                                       val renderRequestTime: Long,
                                       val renderAuthTime: Long,
                                       val requestTime: Long,
                                       val evalAssertionTime: Long,
                                       val totalTime: Long,
                                     )

  /**
    * for any type of request. eg: http, dubbo, sql
    */
  case class JobReportStepItemData(
                                    var id: String,
                                    var title: String,
                                    var itemId: String,
                                    var statis: Statistic,
                                    var `type`: String = ScenarioStep.TYPE_HTTP,
                                    var generator: String = StringUtils.EMPTY, // specify generator type
                                  ) extends BasicReportItemData

  object JobReportStepItemData {

    def parse(title: String, result: HttpResult, itemId: String = null, status: String = null, msg: String = null): JobReportStepItemData = {
      val item = JobReportStepItemData(id = result.caseId, title = title, itemId, statis = result.statis)
      if (StringUtils.isNotEmpty(status)) {
        item.status = status
      } else {
        item.status = if (result.statis.isSuccessful) ReportStepItemStatus.STATUS_PASS else ReportStepItemStatus.STATUS_FAIL
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
  case class ScenarioReportItemData(
                                     var id: String,
                                     var title: String,
                                     var steps: Seq[JobReportStepItemData] = Nil
                                   ) extends BasicReportItemData

}
