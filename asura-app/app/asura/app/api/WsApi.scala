package asura.app.api

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.common.util.StringUtils
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Activity
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.actor.{JobManualActor, JobTestActor}
import asura.core.scenario.actor.ScenarioRunnerActor
import asura.core.scenario.actor.ScenarioRunnerActor.ScenarioTestMessage
import asura.dubbo.actor.TelnetDubboProviderActor
import javax.inject.{Inject, Singleton}
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.WebSocket

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WsApi @Inject()(
                       implicit val system: ActorSystem,
                       implicit val exec: ExecutionContext,
                       implicit val mat: Materializer,
                       val controllerComponents: SecurityComponents,
                       val client: HeaderClient,
                     ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())
  val auth: JwtAuthenticator = client.getAuthenticator().asInstanceOf[JwtAuthenticator]

  def testScenario(group: String, project: String, id: Option[String]) = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val user = profile.getId
          activityActor ! Activity(group, project, user, Activity.TYPE_TEST_SCENARIO, id.getOrElse(StringUtils.EMPTY))
          val testActor = system.actorOf(ScenarioRunnerActor.props())
          WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[ScenarioTestMessage])
        }
      }
    }
  }

  def testJob(group: String, project: String, id: Option[String]) = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val user = profile.getId
          activityActor ! Activity(group, project, user, Activity.TYPE_TEST_JOB, id.getOrElse(StringUtils.EMPTY))
          val testActor = system.actorOf(JobTestActor.props(user))
          WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[JobTestMessage])
        }
      }
    }
  }

  def manualTestJob(group: String, project: String, jobId: String) = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val user = profile.getId
          activityActor ! Activity(group, project, user, Activity.TYPE_TEST_JOB, jobId)
          val testActor = system.actorOf(JobManualActor.props(jobId, user))
          WebSocketMessageHandler.stringToActorEventFlow(testActor)
        }
      }
    }
  }

  def telnetDubbo(address: String, port: Int) = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val user = profile.getId
          activityActor ! Activity(user = user, `type` = Activity.TYPE_TELNET_DUBBO)
          val testActor = system.actorOf(TelnetDubboProviderActor.props(address, port))
          WebSocketMessageHandler.stringToActorEventFlow(testActor)
        }
      }
    }
  }
}
