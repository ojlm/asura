package asura.core.http

object HttpContentTypes {

  val JSON = "application/json"
  val X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
  val TEXT_PLAIN = "text/plain"
  val MULTIPART_FORM_DATA = "multipart/form-data"

  val KEY_CONTENT_TYPE = "Content-Type"

  def isSupport(contentType: String): Boolean = {
    contentType match {
      case JSON | X_WWW_FORM_URLENCODED | TEXT_PLAIN => true
      case _ => false
    }
  }
}
