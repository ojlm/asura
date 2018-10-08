package asura.core.auth

import akka.http.scaladsl.model.HttpRequest
import asura.common.model.BoolErrorRes
import asura.core.es.model.Authorization

import scala.concurrent.Future

trait AuthorizeAndValidate {

  val `type`: String
  val description: String
  val template: String = "{}"

  def authorize(request: HttpRequest, auth: Authorization): Future[HttpRequest]

  def validate(auth: Authorization): BoolErrorRes
}
