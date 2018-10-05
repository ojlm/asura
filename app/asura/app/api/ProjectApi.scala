package asura.app.api

import akka.actor.ActorSystem
import asura.core.cs.model.QueryProject
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Project}
import asura.core.es.service.ProjectService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ProjectApi @Inject()(
                            implicit val system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def get(group: String, id: String) = Action.async { implicit req =>
    ProjectService.getById(group, id).toOkResultByEsOneDoc(Project.generateDocId(group, id))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val project = req.bodyAs(classOf[Project])
    val user = getProfileId()
    project.fillCommonFields(user)
    ProjectService.index(project).map(res => {
      activityActor ! Activity(project.group, res.id, user, Activity.TYPE_NEW_PROJECT, res.id)
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
}
