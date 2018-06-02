package asura.app.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import asura.common.model.ApiResError
import asura.common.util.LogUtils
import asura.core.util.JacksonSupport._
import com.typesafe.scalalogging.Logger

object RoutesExceptionHandler {
  val logger = Logger("RoutesException")
  val handler = ExceptionHandler {
    case t: Throwable =>
      logger.warn(LogUtils.stackTraceToString(t))
      complete(ApiResError(t.getMessage))
  }
}
