package asura.app.routes.es

import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError}
import asura.core.es.EsResponse
import asura.core.es.model.Group
import asura.core.es.service.GroupService
import asura.core.util.JacksonSupport._

import scala.util.{Failure, Success}

object GroupRoutes {

  val groupRoutes =
    pathPrefix("group") {
      path("index") {
        asuraUser() { username =>
          entity(as[Group]) { group =>
            group.fillCommonFields(username)
            onComplete(GroupService.index(group)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
      } ~
        path("update") {
          entity(as[Group]) { group =>
            onComplete(GroupService.updateGroup(group)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("delete") {
          parameters('id) { id =>
            onComplete(GroupService.deleteDoc(id)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("query") {
          onComplete(GroupService.getAll()) {
            case Success(res) =>
              res match {
                case Right(success) => complete(ApiRes(data = EsResponse.toApiData(success.result)))
                case Left(failure) => complete(ApiResError(msg = failure.error.reason))
              }
            case Failure(t) => complete(ApiResError(t.getMessage))
          }
        }
    }
}

