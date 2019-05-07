package asura.core.http

import scala.collection.mutable

case class HttpResponseModel(
                         statusCode: Int,
                         statusMsg: String,
                         headers: mutable.Map[String, String],
                         contentType: String,
                         body: String
                       )
