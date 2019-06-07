package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.common.model.ApiResError
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Project}
import asura.core.es.service.ProjectService
import asura.core.model.{QueryProject, TransferProject}
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProjectApi @Inject()(
                            implicit val system: ActorSystem,
                            val exec: ExecutionContext,
                            val configuration: Configuration,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi {

  val administrators = configuration.getOptional[Seq[String]]("asura.admin").getOrElse(Nil).toSet
  val activityActor = system.actorOf(ActivitySaveActor.props())

  def get(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getById(group, id).toOkResultByEsOneDoc(Project.generateDocId(group, id))
  }

  def delete(group: String, id: String) = Action.async { implicit req =>
    checkPrivilege { user =>
      ProjectService.deleteProject(group, id).map(res => {
        activityActor ! Activity(group, id, user, Activity.TYPE_DELETE_PROJECT, Project.generateDocId(group, id))
        res
      }).toOkResult
    }
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    val user = getProfileId()
    project.fillCommonFields(user)
    ProjectService.index(project).map(res => {
      activityActor ! Activity(project.group, project.id, user, Activity.TYPE_NEW_PROJECT, res.id)
      toActionResultFromAny(res)
    })
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

  def transfer() = Action(parse.byteString).async { implicit req =>
    val op = req.bodyAs(classOf[TransferProject])
    ProjectService.transferProject(op).toOkResult
  }

  private def checkPrivilege(func: String => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    val user = getProfileId()
    val isAllowed = if (administrators.nonEmpty) administrators.contains(user) else true
    if (isAllowed) {
      func(user)
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_NotAllowedContactAdministrator, administrators.mkString(",")))))
    }
  }
}
