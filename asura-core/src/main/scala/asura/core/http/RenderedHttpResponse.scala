package asura.core.http

import asura.core.es.model.JobReportDataItem.DataItemRenderedResponse

case class RenderedHttpResponse(
                                 statusCode: Int,
                                 statusMsg: String,
                                 headers: java.util.ArrayList[java.util.Map[String, String]],
                                 contentType: String,
                                 body: String
                               ) extends DataItemRenderedResponse
