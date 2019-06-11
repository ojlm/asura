package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.common.model.ApiResError
import asura.core.ErrorMessages
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Scenario}
import asura.core.es.service._
import asura.core.model.QueryScenario
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ScenarioApi @Inject()(
                             implicit system: ActorSystem,
                             val exec: ExecutionContext,
                             val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    ScenarioService.getRelativesById(id).toOkResult
  }

  def delete(id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    JobService.containScenario(Seq(id)).flatMap(res => {
      if (res.isSuccess) {
        if (preview.nonEmpty && preview.get) {
          Future.successful(toActionResultFromAny(Map(
            "job" -> EsResponse.toApiData(res.result)
          )))
        } else {
          if (res.result.isEmpty) {
            ScenarioService.deleteDoc(id).toOkResult
          } else {
            Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteScenario))))
          }
        }
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val scenario = req.bodyAs(classOf[Scenario])
    val user = getProfileId()
    scenario.fillCommonFields(user)
    ScenarioService.index(scenario).map(res => {
      activityActor ! Activity(scenario.group, scenario.project, user, Activity.TYPE_NEW_SCENARIO, res.id)
      toActionResultFromAny(res)
    })
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryScenario = req.bodyAs(classOf[QueryScenario])
    ScenarioService.queryScenario(queryScenario).toOkResultByEsList()
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val scenario = req.bodyAs(classOf[Scenario])
    ScenarioService.updateScenario(id, scenario).map(res => {
      activityActor ! Activity(scenario.group, scenario.project, getProfileId(), Activity.TYPE_UPDATE_SCENARIO, id)
      res
    }).toOkResult
  }
}
