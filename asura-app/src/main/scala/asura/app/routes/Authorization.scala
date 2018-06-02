package asura.app.routes

import akka.http.scaladsl.server.RequestContext
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object Authorization {

  val logger = Logger("Authorization")

  def hasPermissions(token: String, requestContext: RequestContext): Future[Boolean] = {
    val request = requestContext.request
    request.copy()
    val uri = request.uri
    logger.info(s"authorize: ${token} => ${uri.path}")
    Future.successful(true)
  }
}
