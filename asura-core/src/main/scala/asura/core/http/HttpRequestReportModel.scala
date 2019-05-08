package asura.core.http

import asura.core.es.model.JobReportDataItem.ReportDataItemRequest

import scala.collection.mutable

case class HttpRequestReportModel(
                                   method: String,
                                   url: String,
                                   headers: mutable.Map[String, String],
                                   body: String
                                 ) extends ReportDataItemRequest
