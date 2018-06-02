package asura.app.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import asura.common.model.{ApiMsg, ApiResError}
import asura.core.util.JacksonSupport._

object RoutesRejectionHandler {
  val handler = RejectionHandler.newBuilder()
    .handleNotFound(complete(ApiResError(ApiMsg.NOT_FOUND)))
    .handle {
      case rejection: Rejection => complete(ApiResError(rejection.toString))
    }
    .result()
}
