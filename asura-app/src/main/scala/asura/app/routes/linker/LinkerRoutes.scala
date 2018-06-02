package asura.app.routes.linker

import akka.http.scaladsl.server.Directives._
import asura.AppConfig
import asura.app.routes.model.Dtabs
import asura.common.model.{ApiRes, ApiResError}
import asura.core.http.HttpEngine
import asura.core.util.JacksonSupport._
import asura.namerd.api.v1.NamerdV1Api

import scala.util.{Failure, Success}

object LinkerRoutes {

  implicit val http = HttpEngine.http

  val linkerRoutes = {
    pathPrefix("http") {
      path("dtabs") {
        get {
          onComplete(NamerdV1Api.getNamespaceDtabs(AppConfig.linkerHttpNs)) {
            case Success(dtabs) => complete(ApiRes(data = dtabs))
            case Failure(t) => complete(ApiResError(t.getMessage))
          }
        } ~
          put {
            entity(as[Dtabs]) { dtabs =>
              onComplete(NamerdV1Api.updateNamespaceDtabs(AppConfig.linkerHttpNs, dtabs.dtabs)) {
                case Success(dtabs) => complete(ApiRes(data = dtabs))
                case Failure(t) => complete(ApiResError(t.getMessage))
              }
            }
          }
      }
    }
  }
}
