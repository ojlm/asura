package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.cs.model.QueryEnv
import asura.core.es.EsResponse
import asura.core.es.model.Environment
import asura.core.es.service.EnvironmentService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class EnvApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    EnvironmentService.getById(id).map { res =>
      res match {
        case Left(failure) => {
          OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(failure).name)))
        }
        case Right(value) => {
          if (value.result.nonEmpty) {
            OkApiRes(ApiRes(data = EsResponse.toSingleApiData(value.result, false)))
          } else {
            OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, id)))
          }
        }
      }
    }
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val env = req.bodyAs(classOf[Environment])
    env.fillCommonFields(getProfileId())
    EnvironmentService.index(env).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryEnv = req.bodyAs(classOf[QueryEnv])
    EnvironmentService.queryEnv(queryEnv).map(toActionResult(_, false))
  }
}
