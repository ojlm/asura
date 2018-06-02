package asura.core.http

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaTypes}

object HttpContentTypes {
  val `x-www-form-urlencoded(UTF-8)` = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`)

  val JSON = "application/json"
  val X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
  val TEXT_PLAIN = "text/plain"

  val KEY_CONTENT_TYPE = "Content-Type"
}
