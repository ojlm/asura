package asura.core.http

import asura.core.es.model.JobReportDataItem.ReportDataItemResponse

import scala.collection.mutable

case class HttpResponseReportModel(
                                    statusCode: Int,
                                    statusMsg: String,
                                    headers: mutable.Map[String, String],
                                    contentType: String,
                                    body: String
                                  ) extends ReportDataItemResponse
