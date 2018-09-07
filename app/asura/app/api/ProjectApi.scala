package asura.app.api

import asura.core.cs.model.QueryProject
import asura.core.es.model.Project
import asura.core.es.service.ProjectService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ProjectApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def get(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getById(group, id).toOkResultByEsOneDoc(Project.generateDocId(group, id))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    project.fillCommonFields(getProfileId())
    ProjectService.index(project).toOkResult
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    ProjectService.updateProject(project).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryProject = req.bodyAs(classOf[QueryProject])
    ProjectService.queryProject(queryProject).toOkResultByEsList(false)
  }

  def getOpenApi(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getOpenApi(group, id).toOkResultByEsOneDoc(Project.generateDocId(group, id))
  }

  def updateOpenApi(group: String, id: String) = Action(parse.byteString).async { implicit req =>
    val openapi = req.body.decodeString("UTF-8")
    ProjectService.updateOpenApi(group, id, openapi).toOkResult
  }
}
