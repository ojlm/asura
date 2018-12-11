package asura.core.cs

import scala.collection.mutable

case class CaseResponse(
                         statusCode: Int,
                         statusMsg: String,
                         headers: mutable.Map[String, String],
                         contentType: String,
                         body: String
                       )
