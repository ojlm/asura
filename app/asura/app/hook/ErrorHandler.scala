package asura.app.hook

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiResError
import asura.common.util.{LogUtils, StringUtils}
import javax.inject.Singleton
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {

  lazy val logger = LoggerFactory.getLogger(classOf[ErrorHandler])

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(OkApiRes(ApiResError(s"${statusCode}${if (StringUtils.isNotEmpty(message)) ": " + message else ""}")))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val logStack = LogUtils.stackTraceToString(exception)
    logger.warn(logStack)
    Future.successful(OkApiRes(ApiResError(exception.getMessage)))
  }
}
