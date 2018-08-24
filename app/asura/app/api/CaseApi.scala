package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.cs.model.QueryCase
import asura.core.es.EsResponse
import asura.core.es.model.Case
import asura.core.es.service.CaseService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class CaseApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    CaseService.getById(id).map { res =>
      res match {
        case Left(failure) => {
          OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(failure).name)))
        }
        case Right(value) => {
          if (value.result.nonEmpty) {
            OkApiRes(ApiRes(data = EsResponse.toSingleApiData(value.result, true)))
          } else {
            OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, id)))
          }
        }
      }
    }
  }

  def delete(id: String) = Action.async { implicit req =>
    CaseService.deleteDoc(id).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def put() = Action(parse.tolerantText).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    cs.fillCommonFields(getProfileId())
    CaseService.index(cs).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def query() = Action(parse.tolerantText).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    CaseService.queryCase(queryCase).map(toActionResult(_, true))
  }
}
