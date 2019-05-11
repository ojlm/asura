package asura.core.runtime

import asura.core.assertion.engine.Statistic
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.es.model.JobReportDataItem.{ReportDataItemRequest, ReportDataItemResponse}

trait AbstractResult {

  def docId: String

  def assert: Map[String, Any]

  def context: java.util.Map[Any, Any]

  def request: ReportDataItemRequest

  def response: ReportDataItemResponse

  def metrics: JobReportStepItemMetrics

  def statis: Statistic

  def result: java.util.Map[_, _]

  // generator type
  def generator: String
}
