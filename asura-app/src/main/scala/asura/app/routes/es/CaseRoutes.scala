package asura.app.routes.es

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.LogUtils
import asura.core.cs.CaseRunner
import asura.core.cs.model.QueryCase
import asura.core.es.EsResponse
import asura.core.es.model.Case
import asura.core.es.service.CaseService
import asura.core.util.JacksonSupport._
import asura.routes.model.{ByIds, FullTextSearch, UpdateCase}
import com.typesafe.scalalogging.Logger

import scala.util.{Failure, Success}

object CaseRoutes {

  val logger = Logger("CaseRoutes")

  val caseRoutes =
    pathPrefix("case") {
      path("index") {
        asuraUser() { username =>
          entity(as[Case]) { cs =>
            cs.fillCommonFields(username)
            onComplete(CaseService.index(cs)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
      } ~
        path("update") {
          entity(as[UpdateCase]) { req =>
            onComplete(CaseService.updateCs(req.id, req.cs)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("delete") {
          parameters('id) { id =>
            onComplete(CaseService.deleteDoc(id)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("query") {
          entity(as[QueryCase]) { query =>
            onComplete(CaseService.queryCase(query)) {
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
        path("test") {
          method(HttpMethods.POST) {
            entity(as[Case]) { cs =>
              onComplete(CaseRunner.test("test", cs)) {
                case Success(res) => complete(ApiRes(data = res))
                case Failure(t) =>
                  logger.warn(LogUtils.stackTraceToString(t))
                  complete(ApiResError(t.getMessage))
              }
            }
          }
        } ~
        path("search") {
          entity(as[FullTextSearch]) { search =>
            onComplete(CaseService.searchText(search.text)) {
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
            onComplete(CaseService.getByIds(byIds.ids)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
    }
}
