package asura.core.cs

import akka.http.scaladsl.model.{HttpHeader, HttpMethod, HttpRequest, Uri, HttpMethods => AkkaHttpMethods}
import asura.common.exceptions.IllegalRequestException
import asura.common.util.StringUtils
import asura.core.auth.AuthManager
import asura.core.es.model.{Authorization, Case, Environment, RestApi}
import asura.core.http.{EntityUtils, HeaderUtils, HttpMethods, UriUtils}

import scala.collection.immutable
import scala.concurrent.Future

object CaseParser {

  def toHttpRequest(cs: Case, context: CaseContext, api: RestApi, env: Environment = null): Future[HttpRequest] = {
    var method: HttpMethod = null
    val headers: immutable.Seq[HttpHeader] = HeaderUtils.toHeaders(cs, context, api, env)
    val request = cs.request
    if (null == request) {
      method = AkkaHttpMethods.GET
    } else {
      method = HttpMethods.toAkkaMethod(request.method)
    }
    val uri: Uri = UriUtils.toUri(cs, context, api, env)
    val entity = EntityUtils.toEntity(cs, context)
    val notAuthRequest = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
    val authUsed: Authorization = if (cs.useEnv) {
      if (null == env) {
        throw new IllegalRequestException("Need a env, but get null")
      } else {
        env.auth
      }
    } else {
      cs.request.auth
    }
    if (null != authUsed && StringUtils.isNotEmpty(authUsed.`type`)) {
      val operator = AuthManager(authUsed.`type`)
      if (operator.nonEmpty) {
        operator.get.authorize(notAuthRequest, authUsed)
      } else {
        throw new IllegalRequestException(s"Can not find registered auth type : ${authUsed.`type`}")
      }
    } else {
      Future.successful(notAuthRequest)
    }
  }
}
