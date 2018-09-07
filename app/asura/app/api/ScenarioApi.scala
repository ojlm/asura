package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.cs.model.QueryScenario
import asura.core.es.model.Scenario
import asura.core.es.service.ScenarioService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ScenarioApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    ScenarioService.getById(id).map(toActionResultWithSingleData(_, id))
  }

  def delete(id: String) = Action.async { implicit req =>
    ScenarioService.deleteDoc(id).map(res => OkApiRes(ApiRes(data = res)))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val scenario = req.bodyAs(classOf[Scenario])
    scenario.fillCommonFields(getProfileId())
    ScenarioService.index(scenario).map(res => OkApiRes(ApiRes(data = res)))
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryScenario = req.bodyAs(classOf[QueryScenario])
    ScenarioService.queryScenario(queryScenario).map(toActionResult(_, true))
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val scenario = req.bodyAs(classOf[Scenario])
    ScenarioService.updateScenario(id, scenario).map(res => OkApiRes(ApiRes(data = res)))
  }
}
