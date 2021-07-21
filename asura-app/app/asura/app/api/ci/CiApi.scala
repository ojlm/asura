package asura.app.api.ci

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import asura.app.api.ci.CiApi.OverrideImports
import asura.common.actor.SenderMessage
import asura.core.CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.core.actor.flow.WebSocketMessageHandler.completionMatcher
import asura.core.ci.{CiManager, CiTriggerEventMessage}
import asura.core.es.model.JobReportData.ScenarioReportItemData
import asura.core.es.model.{Scenario, VariablesImportItem}
import asura.core.es.service.ScenarioService
import asura.core.job.actor.JobCiActor
import asura.core.runtime.RuntimeContext
import asura.core.scenario.actor.ScenarioRunnerActor
import asura.core.scenario.actor.ScenarioRunnerActor.ScenarioTestJobMessage
import asura.play.api.BaseApi
import org.pac4j.play.scala.SecurityComponents
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Codec, WebSocket}

@Singleton
class CiApi @Inject()(
                       implicit val system: ActorSystem,
                       implicit val exec: ExecutionContext,
                       implicit val mat: Materializer,
                       val controllerComponents: SecurityComponents
                     ) extends BaseApi {

  implicit val codec = Codec.utf_8

  def home() = Action {
    Ok("CI")
  }

  def trigger() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[CiTriggerEventMessage])
    CiManager.eventSource(msg)
    Future.successful(Ok("OK"))
  }

  def jobWS(id: String) = WebSocket.accept[String, String] { implicit req =>
    ActorFlow.actorRef(out => JobCiActor.props(id, out))
  }

  def jobSSE(id: String) = Action {
    val ciActor = system.actorOf(JobCiActor.props(id))
    val source = Source.actorRef[String](
      completionMatcher,
      PartialFunction.empty,
      BaseApi.DEFAULT_SOURCE_BUFFER_SIZE,
      OverflowStrategy.dropHead
    ).mapMaterializedValue(ref => ciActor ! SenderMessage(ref))
    Ok.chunked(source via EventSource.flow)
      .as(ContentTypes.EVENT_STREAM)
      .withHeaders(BaseApi.responseNoCacheHeaders: _*)
  }

  def runGetScenario(id: String) = Action(parse.byteString).async { implicit req =>
    ScenarioService.getScenarioById(id).flatMap(scenario => {
      runScenario(id, scenario, scenario.imports)
    })
  }

  def runPostScenario(id: String) = Action(parse.byteString).async { implicit req =>
    ScenarioService.getScenarioById(id).flatMap(scenario => {
      val body = req.bodyAs(classOf[OverrideImports])
      val imports = if (body != null && body.imports != null) {
        if (scenario.imports != null) {
          scenario.imports ++ body.imports
        } else {
          body.imports
        }
      } else {
        scenario.imports
      }
      runScenario(id, scenario, imports)
    })
  }

  private def runScenario(id: String, scenario: Scenario, imports: Seq[VariablesImportItem]) = {
    val msg = ScenarioTestJobMessage(
      summary = scenario.summary,
      steps = scenario.steps,
      storeHelper = null,
      runtimeContext = RuntimeContext(),
      imports = imports,
      exports = scenario.exports,
      failFast = scenario.failFast,
    )
    (system.actorOf(ScenarioRunnerActor.props(id)) ? msg)
      .asInstanceOf[Future[ScenarioReportItemData]]
      .map(item => {
        Map("result" -> item, "context" -> msg.runtimeContext.rawContext)
      }).toOkResult
  }

}

object CiApi {

  case class OverrideImports(imports: Seq[VariablesImportItem])

}
