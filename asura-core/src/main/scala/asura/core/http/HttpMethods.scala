package asura.core.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods => AkkaHttpMethods}

object HttpMethods {

  val CONNECT = "CONNECT"
  val DELETE = "DELETE"
  val GET = "GET"
  val HEAD = "HEAD"
  val OPTIONS = "OPTIONS"
  val PATCH = "PATCH"
  val POST = "POST"
  val PUT = "PUT"
  val TRACE = "TRACE"


  def toAkkaMethod(m: String): HttpMethod = {
    m match {
      case CONNECT => AkkaHttpMethods.CONNECT
      case DELETE => AkkaHttpMethods.DELETE
      case GET => AkkaHttpMethods.GET
      case HEAD => AkkaHttpMethods.HEAD
      case OPTIONS => AkkaHttpMethods.OPTIONS
      case PATCH => AkkaHttpMethods.PATCH
      case POST => AkkaHttpMethods.POST
      case PUT => AkkaHttpMethods.PUT
      case TRACE => AkkaHttpMethods.TRACE
      case _ => AkkaHttpMethods.GET
    }
  }
}
