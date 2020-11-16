package asura.core.http

import akka.http.scaladsl.model.{HttpMethods => AkkaHttpMethods, _}
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{Authorization, HttpStepRequest}
import asura.core.runtime.{RuntimeContext, RuntimeMetrics}

import scala.collection.immutable
import scala.concurrent.Future

object HttpParser {

  def toHttpRequest(httpRequest: HttpStepRequest, context: RuntimeContext)
                   (implicit metrics: RuntimeMetrics): Future[HttpRequest] = {
    var method: HttpMethod = null
    val request = httpRequest.request
    if (null == request) {
      method = AkkaHttpMethods.GET
    } else {
      method = HttpMethods.toAkkaMethod(request.method)
    }
    val uri: Uri = UriUtils.toUri(httpRequest, context)
    val headers: immutable.Seq[HttpHeader] = HeaderUtils.toHeaders(httpRequest, context)
    val entityFuture = if (AkkaHttpMethods.GET != method) {
      EntityUtils.toEntity(httpRequest, context)
    } else {
      Future.successful(HttpEntity.Empty)
    }
    entityFuture.flatMap(entity => {
      val notAuthoredRequest = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
      metrics.renderRequestEnd()
      val authUsed: Seq[Authorization] = if (null != context.options && null != context.options.getUsedEnv()) {
        context.options.getUsedEnv().auth
      } else {
        Nil
      }
      if (null != authUsed && authUsed.nonEmpty) {
        metrics.renderAuthBegin()
        authUsed.foldLeft(Future.successful(notAuthoredRequest))((futureRequest, auth) => {
          for {
            initialAuthoredRequest <- futureRequest
            authoredRequest <- {
              val operator = AuthManager(auth.`type`)
              if (operator.nonEmpty) {
                operator.get.authorize(initialAuthoredRequest, auth)
              } else {
                ErrorMessages.error_NotRegisteredAuth(auth.`type`).toFutureFail
              }
            }
          } yield authoredRequest
        }).map(req => {
          metrics.renderAuthEnd()
          req
        })
      } else {
        Future.successful(notAuthoredRequest)
      }
    })
  }
}
