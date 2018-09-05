package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.ErrorMessages
import asura.core.cs.model.QueryProject
import asura.core.es.model.Project
import asura.core.es.service.ProjectService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ProjectApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getById(group, id).map(res => toActionResultWithSingleData(res, Project.generateDocId(group, id), false))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    project.fillCommonFields(getProfileId())
    ProjectService.index(project).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    ProjectService.updateProject(project).map { res =>
      OkApiRes(ApiRes(data = res, msg = getI18nMessage(ErrorMessages.error_UpdateSuccess.name)))
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryProject = req.bodyAs(classOf[QueryProject])
    ProjectService.queryProject(queryProject).map(toActionResult(_, false))
  }

  def getOpenApi(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getOpenApi(group, id).map(res => toActionResultWithSingleData(res, Project.generateDocId(group, id), false))
  }

  def updateOpenApi(group: String, id: String) = Action(parse.byteString).async { implicit req =>
    val openapi = req.body.decodeString("UTF-8")
    ProjectService.updateOpenApi(group, id, openapi).map { res =>
      OkApiRes(ApiRes(data = res, msg = getI18nMessage(ErrorMessages.error_UpdateSuccess.name)))
    }
  }
}
