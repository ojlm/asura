package asura.app.routes.es

import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError, BoolErrorRes}
import asura.common.util.StringUtils
import asura.core.auth.AuthManager
import asura.core.es.EsResponse
import asura.core.es.model.Environment
import asura.core.es.service.EnvironmentService
import asura.core.util.JacksonSupport._
import asura.routes.model.UpdateEnv

import scala.util.{Failure, Success}

object EnvRoutes {

  val envRoutes =
    pathPrefix("env") {
      path("index") {
        asuraUser() { username =>
          entity(as[Environment]) { env =>
            env.fillCommonFields(username)
            val (isOK, errMsg) = validate(env)
            if (isOK) {
              onComplete(EnvironmentService.index(env)) {
                case Success(res) => complete(defaultEsResponseHandler(res))
                case Failure(t) => complete(ApiResError(t.getMessage))
              }
            } else {
              complete(ApiResError(errMsg))
            }
          }
        }
      } ~
        path("update") {
          entity(as[UpdateEnv]) { updateEnv =>
            val (isOK, errMsg) = validate(updateEnv.env)
            if (isOK) {
              onComplete(EnvironmentService.updateEnv(updateEnv.id, updateEnv.env)) {
                case Success(res) => complete(defaultEsResponseHandler(res))
                case Failure(t) => complete(ApiResError(t.getMessage))
              }
            } else {
              complete(ApiResError(errMsg))
            }
          }
        } ~
        path("delete") {
          parameters('id) { id =>
            onComplete(EnvironmentService.deleteDoc(id)) {
              case Success(res) => complete(defaultEsResponseHandler(res))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("query") {
          parameters('project) { project =>
            onComplete(EnvironmentService.getAll(project)) {
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

  def validate(env: Environment): BoolErrorRes = {
    if (null != env) {
      val auth = env.auth
      if (null != auth && StringUtils.isNotEmpty(auth.`type`)) {
        val maybeValidate = AuthManager(auth.`type`)
        if (maybeValidate.nonEmpty) {
          maybeValidate.get.validate(auth)
        } else {
          (false, "Unknown Authorization Type")
        }
      } else {
        (true, null)
      }
    } else {
      (false, "Null input")
    }
  }
}
