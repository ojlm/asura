package asura.app.api

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.actor.{JobManualActor, JobTestActor, ScenarioTestActor}
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

  val auth: JwtAuthenticator = client.getAuthenticator().asInstanceOf[JwtAuthenticator]

  def testScenario() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val testActor = system.actorOf(ScenarioTestActor.props(profile.getId))
          WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[JobTestMessage])
        }
      }
    }
  }

  def testJob() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val testActor = system.actorOf(JobTestActor.props(profile.getId))
          WebSocketMessageHandler.stringToActorEventFlow(testActor, classOf[JobTestMessage])
        }
      }
    }
  }

  def manualTestJob(jobId: String) = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      val profile = getWsProfile(auth)
      if (null == profile) {
        Left(Forbidden)
      } else {
        Right {
          val testActor = system.actorOf(JobManualActor.props(profile.getId, jobId))
          WebSocketMessageHandler.stringToActorEventFlow(testActor)
        }
      }
    }
  }
}
