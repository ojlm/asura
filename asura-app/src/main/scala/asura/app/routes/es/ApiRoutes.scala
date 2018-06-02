package asura.app.routes.es

import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError}
import asura.core.api.openapi.OpenAPI
import asura.core.es.EsResponse
import asura.core.es.model.RestApi
import asura.core.es.service.ApiService
import asura.core.es.service.ApiService.ApiUpdate
import asura.core.util.JacksonSupport._
import asura.routes.model.{ApiImport, ByIds}

import scala.util.{Failure, Success}

object ApiRoutes {

  val apiRoutes =
    pathPrefix("api") {
      path("index") {
        asuraUser() { username =>
          entity(as[RestApi]) { api =>
            api.fillCommonFields(username)
            onComplete(ApiService.index(api)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
      } ~
        path("update") {
          entity(as[ApiUpdate]) { apiUpdate =>
            onComplete(ApiService.updateApi(apiUpdate)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("delete") {
          parameters('id) { (id) =>
            onComplete(ApiService.deleteDoc(id)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("query") {
          parameters('project) { project =>
            onComplete(ApiService.getAll(project)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("getByIds") {
          entity(as[ByIds]) { byIds =>
            onComplete(ApiService.getById(byIds.ids)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("import") {
          asuraUser() { username =>
            entity(as[ApiImport]) { req =>
              val apis = OpenAPI.v2ToApi(req.openApi, req.group, req.project, username)
              if (req.preview) {
                complete(ApiRes(data = apis))
              } else {
                onComplete(ApiService.index(apis)) {
                  case Success(res) => complete {
                    res match {
                      case Right(success) => ApiRes(data = success)
                      case Left(failure) => ApiResError(failure.error.reason)
                    }
                  }
                  case Failure(t) => complete(ApiResError(t.getMessage))
                }
              }
            }
          }
        }
    }
}
