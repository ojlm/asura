package asura.app.api

import akka.actor.ActorSystem
import asura.core.cs.model.QueryScenario
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Scenario}
import asura.core.es.service.ScenarioService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ScenarioApi @Inject()(
                             implicit system: ActorSystem,
                             val exec: ExecutionContext,
                             val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    ScenarioService.getById(id).toOkResultByEsOneDoc(id)
  }

  def delete(id: String) = Action.async { implicit req =>
    ScenarioService.deleteDoc(id).toOkResult
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
    ScenarioService.updateScenario(id, scenario).toOkResult
  }
}
