package asura.app.hook

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiCode, ApiRes, ApiResError}
import asura.common.util.{LogUtils, StringUtils}
import asura.core.ErrorMessages.ErrorMessageException
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject()(messagesApi: MessagesApi, langs: Langs) extends HttpErrorHandler {

  lazy val logger = LoggerFactory.getLogger(classOf[ErrorHandler])

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(OkApiRes(ApiResError(s"${statusCode}${if (StringUtils.isNotEmpty(message)) ": " + message else ""}")))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val logStack = LogUtils.stackTraceToString(exception)
    logger.warn(logStack)
    exception match {
      case errMsgException: ErrorMessageException =>
        val requestLocal = request.headers.get("Local")
        implicit val lang = if (requestLocal.nonEmpty) {
          langs.availables.find(_.code == requestLocal.get).getOrElse(langs.availables.head)
        } else {
          langs.availables.head
        }
        val errMsg = messagesApi(errMsgException.error.name, errMsgException.error.errMsg)
        Future.successful(OkApiRes(ApiRes(code = ApiCode.ERROR, msg = errMsg, data = logStack)))
      case _ =>
        Future.successful(OkApiRes(ApiRes(code = ApiCode.ERROR, msg = exception.getMessage, data = logStack)))
    }
  }
}
