package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.common.model.ApiResError
import asura.core.ErrorMessages
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, Scenario}
import asura.core.es.service._
import asura.core.model.QueryScenario
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScenarioApi @Inject()(
                             implicit system: ActorSystem,
                             val exec: ExecutionContext,
                             val controllerComponents: SecurityComponents,
                             val permissionAuthProvider: PermissionAuthProvider,
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      ScenarioService.getRelativesById(id).toOkResult
    }
  }

  def copyById(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_CLONE) { user =>
      ScenarioService.copyById(id, user).toOkResult
    }
  }

  def delete(group: String, project: String, id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_REMOVE) { _ =>
      JobService.containScenario(Seq(id)).flatMap(res => {
        if (res.isSuccess) {
          if (preview.nonEmpty && preview.get) {
            Future.successful(toActionResultFromAny(Map(
              "job" -> EsResponse.toApiData(res.result)
            )))
          } else {
            if (res.result.isEmpty) {
              ScenarioService.deleteDoc(id).flatMap(_ => {
                FavoriteService.deleteScenario(id)
              }).toOkResult
            } else {
              Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteScenario))))
            }
          }
        } else {
          ErrorMessages.error_EsRequestFail(res).toFutureFail
        }
      })
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val scenario = req.bodyAs(classOf[Scenario])
      scenario.group = group
      scenario.project = project
      scenario.fillCommonFields(user)
      ScenarioService.index(scenario).map(res => {
        activityActor ! Activity(scenario.group, scenario.project, user, Activity.TYPE_NEW_SCENARIO, res.id)
        toActionResultFromAny(res)
      })
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_LIST) { _ =>
      val q = req.bodyAs(classOf[QueryScenario])
      ScenarioService.queryScenario(q).toOkResultByEsList()
    }
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val scenario = req.bodyAs(classOf[Scenario])
      scenario.group = group
      scenario.project = project
      ScenarioService.updateScenario(id, scenario).map(res => {
        activityActor ! Activity(group, project, user, Activity.TYPE_UPDATE_SCENARIO, id)
        res
      }).toOkResult
    }
  }
}
