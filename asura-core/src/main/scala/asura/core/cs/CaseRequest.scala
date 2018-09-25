package asura.core.cs

import scala.collection.mutable

case class CaseRequest(
                        method: String,
                        url: String,
                        headers: mutable.Map[String, String],
                        body: String
                      )
