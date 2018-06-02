package asura.core.auth

import akka.http.scaladsl.model.HttpRequest
import asura.core.es.model.Authorization

case class AuthHttpRequestMessage(request: HttpRequest, auth: Authorization)
