package asura.core.cs

import akka.http.scaladsl.model.{HttpHeader, HttpMethod, HttpRequest, Uri, HttpMethods => AkkaHttpMethods}
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{Authorization, Case}
import asura.core.http.{EntityUtils, HeaderUtils, HttpMethods, UriUtils}

import scala.collection.immutable
import scala.concurrent.Future

object CaseParser {

  def toHttpRequest(cs: Case, context: CaseContext): Future[HttpRequest] = {
    var method: HttpMethod = null
    val headers: immutable.Seq[HttpHeader] = HeaderUtils.toHeaders(cs, context)
    val request = cs.request
    if (null == request) {
      method = AkkaHttpMethods.GET
    } else {
      method = HttpMethods.toAkkaMethod(request.method)
    }
    val uri: Uri = UriUtils.toUri(cs, context)
    val entity = EntityUtils.toEntity(cs, context)
    val notAuthoredRequest = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
    val authUsed: Seq[Authorization] = if (null != context.options && null != context.options.getUsedEnv()) {
      context.options.getUsedEnv().auth
    } else {
      Nil
    }
    if (null != authUsed && authUsed.nonEmpty) {
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
      })
    } else {
      Future.successful(notAuthoredRequest)
    }
  }
}
