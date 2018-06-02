package asura.app.routes.es

import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError}
import asura.core.es.EsResponse
import asura.core.es.model.Project
import asura.core.es.service.ProjectService
import asura.core.util.JacksonSupport._

import scala.util.{Failure, Success}

object ProjectRoutes {

  val projectRoutes =
    pathPrefix("project") {
      path("index") {
        asuraUser() { username =>
          entity(as[Project]) { project =>
            project.fillCommonFields(username)
            onComplete(ProjectService.index(project)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
      } ~
        path("update") {
          entity(as[Project]) { req =>
            onComplete(ProjectService.updateProject(req)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("delete") {
          parameters('id) { id =>
            onComplete(ProjectService.deleteDoc(id)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("query") {
          parameters('group) { group =>
            onComplete(ProjectService.getAll(group)) {
              case Success(res) =>
                res match {
                  case Right(success) => complete(ApiRes(data = EsResponse.toApiData(success.result)))
                  case Left(failure) => complete(ApiResError(failure.error.reason))
                }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
    }
}
