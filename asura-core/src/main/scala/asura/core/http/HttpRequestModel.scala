package asura.core.http

import scala.collection.mutable

case class HttpRequestModel(
                        method: String,
                        url: String,
                        headers: mutable.Map[String, String],
                        body: String
                      )
