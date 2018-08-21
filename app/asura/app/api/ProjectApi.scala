package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.cs.model.QueryProject
import asura.core.es.EsResponse
import asura.core.es.model.Project
import asura.core.es.service.ProjectService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ProjectApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    ProjectService.getById(id).map { res =>
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

  def put() = Action(parse.tolerantText).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    project.fillCommonFields(getProfileId())
    ProjectService.index(project).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def query() = Action(parse.tolerantText).async { implicit req =>
    val queryProject = req.bodyAs(classOf[QueryProject])
    ProjectService.queryProject(queryProject).map(toActionResult(_, false))
  }
}
