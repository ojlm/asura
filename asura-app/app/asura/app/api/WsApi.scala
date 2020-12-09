package asura.app.api

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.common.util.StringUtils
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Activity
import asura.core.es.model.Permissions.Functions
import asura.core.http.actor.HttpRunnerActor
import asura.core.http.actor.HttpRunnerActor.TestCaseMessage
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.actor.{JobManualActor, JobTestActor}
import asura.core.scenario.actor.ScenarioRunnerActor
import asura.core.scenario.actor.ScenarioRunnerActor.ScenarioTestWebMessage
import asura.core.security.PermissionAuthProvider
import asura.dubbo.actor.TelnetDubboProviderActor
import javax.inject.{Inject, Singleton}
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext

@Singleton
class WsApi @Inject()(
                       implicit val system: ActorSystem,
                       implicit val exec: ExecutionContext,
                       implicit val mat: Materializer,
                       val controllerComponents: SecurityComponents,
                       val client: HeaderClient,
                       val permissionAuthProvider: PermissionAuthProvider,
                     ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def testHttp(group: String, project: String, id: Option[String]) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { user =>
      Right {
        val docId = id.getOrElse(StringUtils.EMPTY)
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_CASE, docId)
        val testActor = system.actorOf(HttpRunnerActor.props())
        WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[TestCaseMessage])
      }
    }
  }

  def testScenario(group: String, project: String, id: Option[String]) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { user =>
      Right {
        val scenarioId = id.getOrElse(StringUtils.EMPTY)
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_SCENARIO, scenarioId)
        val testActor = system.actorOf(ScenarioRunnerActor.props(scenarioId))
        WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[ScenarioTestWebMessage])
      }
    }
  }

  def testJob(group: String, project: String, id: Option[String]) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.JOB_EXEC) { user =>
      Right {
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_JOB, id.getOrElse(StringUtils.EMPTY))
        val testActor = system.actorOf(JobTestActor.props(user))
        WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[JobTestMessage])
      }
    }
  }

  def manualTestJob(group: String, project: String, jobId: String) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.JOB_EXEC) { user =>
      Right {
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_JOB, jobId)
        val testActor = system.actorOf(JobManualActor.props(jobId, user))
        WebSocketMessageHandler.stringToActorEventFlow(testActor)
      }
    }
  }

  def telnetDubbo(group: String, project: String, address: String, port: Int) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { user =>
      Right {
        activityActor ! Activity(user = user, `type` = Activity.TYPE_TELNET_DUBBO)
        val testActor = system.actorOf(TelnetDubboProviderActor.props(address, port))
        WebSocketMessageHandler.stringToActorEventFlow(testActor)
      }
    }
  }
}
