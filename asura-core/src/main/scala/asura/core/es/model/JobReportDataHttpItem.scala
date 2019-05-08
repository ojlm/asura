package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.http.{HttpRequestModel, HttpResponseModel}
import com.sksamuel.elastic4s.mappings._

// for http report data
case class JobReportDataHttpItem(
                                  reportId: String,
                                  caseId: String,
                                  scenarioId: String,
                                  jobId: String,
                                  metrics: JobReportStepItemMetrics,
                                  request: HttpRequestModel,
                                  response: HttpResponseModel,
                                  assertions: Map[String, Any],
                                  assertionsResult: java.util.Map[_, _],
                                  generator: String = StringUtils.EMPTY,
                                ) extends JobReportDataItem {

}

object JobReportDataHttpItem extends IndexSetting {

  val INDEX_DATE_TIME_PATTERN = "yyyy.MM.dd"
  val Index: String = s"${EsConfig.IndexPrefix}job-report-item"
  override val shards: Int = 5
  override val replicas: Int = 0
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_CASE_ID),
      KeywordField(name = FieldKeys.FIELD_SCENARIO_ID),
      ObjectField(name = FieldKeys.FIELD_METRICS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_REQUEST, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_RESPONSE, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_ASSERTIONS_RESULT, dynamic = Some("false")),
      KeywordField(name = FieldKeys.FIELD_GENERATOR),
    )
  )
}
