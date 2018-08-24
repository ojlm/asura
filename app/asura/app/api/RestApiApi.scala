package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.cs.model.QueryApi
import asura.core.es.EsResponse
import asura.core.es.model.RestApi
import asura.core.es.service.ApiService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class RestApiApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    ApiService.getById(id).map { res =>
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

  def getOne() = Action(parse.tolerantText).async { implicit req =>
    val api = req.bodyAs(classOf[RestApi])
    ApiService.getOne(api).map { res =>
      res match {
        case Left(failure) => {
          OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(failure).name)))
        }
        case Right(value) => {
          if (value.result.nonEmpty) {
            OkApiRes(ApiRes(data = EsResponse.toSingleApiData(value.result, false)))
          } else {
            OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, api.generateId())))
          }
        }
      }
    }
  }

  def put() = Action(parse.tolerantText).async { implicit req =>
    val api = req.bodyAs(classOf[RestApi])
    api.fillCommonFields(getProfileId())
    ApiService.index(api).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def query() = Action(parse.tolerantText).async { implicit req =>
    val query = req.bodyAs(classOf[QueryApi])
    ApiService.queryApi(query).map(toActionResult(_, true))
  }
}
