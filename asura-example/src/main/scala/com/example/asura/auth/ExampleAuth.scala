package com.example.asura.auth

import akka.http.scaladsl.model.HttpRequest
import asura.core.auth.AuthorizeAndValidate
import asura.core.es.model.Authorization
import play.api.Configuration

import scala.concurrent.Future

class ExampleAuth(config: Configuration) extends AuthorizeAndValidate {

  override val `type`: String = "ExampleAuth"
  override val description: String =
    """# ExampleAuth do nothing
      |markdown syntax
    """.stripMargin
  override val template: String =
    """{
      |    "appKey" : "",
      |    "appSecret" : ""
      |}
    """.stripMargin

  override def authorize(request: HttpRequest, auth: Authorization): Future[HttpRequest] = {
    Future.successful(request)
  }

  override def validate(auth: Authorization): (Boolean, String) = (true, null)
}
