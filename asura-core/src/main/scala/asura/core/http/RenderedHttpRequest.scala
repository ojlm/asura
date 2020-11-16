package asura.core.http

import asura.core.es.model.JobReportDataItem.DataItemRenderedRequest
import asura.core.es.model.MediaObject

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class RenderedHttpRequest(
                                method: String,
                                var url: String = null,
                                var headers: mutable.ArrayBuffer[Map[String, String]] = ArrayBuffer(),
                                var body: MediaObject = null
                              ) extends DataItemRenderedRequest
