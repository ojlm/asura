package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.es.model.JobReportDataItem.{DataItemRenderedRequest, DataItemRenderedResponse}
import asura.core.runtime.AbstractResult
import com.sksamuel.elastic4s.mappings._

// save all type results, eg: http, sql, dubbo
case class JobReportDataItem(
                              reportId: String,
                              caseId: String, // doc id
                              scenarioId: String,
                              jobId: String,
                              `type`: String,
                              var metrics: JobReportStepItemMetrics,
                              request: DataItemRenderedRequest,
                              response: DataItemRenderedResponse,
                              var assertions: Map[String, Any],
                              var assertionsResult: java.util.Map[_, _],
                              var generator: String = StringUtils.EMPTY, // specify generator type
                            ) {

}

object JobReportDataItem extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}job-report-item"
  override val shards: Int = 5
  override val replicas: Int = 0
  val mappings: MappingDefinition = Es6MappingDefinition(
    Seq(
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_CASE_ID),
      KeywordField(name = FieldKeys.FIELD_SCENARIO_ID),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      ObjectField(name = FieldKeys.FIELD_METRICS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_REQUEST, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_RESPONSE, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS_RESULT, dynamic = Some("false")),
      KeywordField(name = FieldKeys.FIELD_GENERATOR),
    )
  )

  // rendered request
  trait DataItemRenderedRequest

  trait DataItemRenderedResponse

  def parse(
             jobId: String,
             reportId: String,
             scenarioId: String,
             `type`: String,
             result: AbstractResult
           ): JobReportDataItem = {
    JobReportDataItem(
      reportId = reportId,
      caseId = result.docId,
      scenarioId = scenarioId,
      jobId = jobId,
      `type` = `type`,
      metrics = result.metrics,
      request = result.request,
      response = result.response,
      assertions = result.assert,
      assertionsResult = result.result,
      generator = StringUtils.notEmptyElse(result.generator, StringUtils.EMPTY)
    )
  }
}
