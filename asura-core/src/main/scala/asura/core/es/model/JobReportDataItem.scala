package asura.core.es.model

import asura.core.es.model.JobReportData.JobReportStepItemMetrics

trait JobReportDataItem {
  val reportId: String
  // doc id
  val caseId: String
  val scenarioId: String
  val jobId: String
  var metrics: JobReportStepItemMetrics
  val request: Any
  val response: Any
  var assertions: Map[String, Any]
  var assertionsResult: java.util.Map[_, _]
  // specify generator type
  var generator: String
}
