package asura.app.api

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import asura.common.actor.ActorEvent
import asura.core.actor.messages.SenderMessage
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.actor.{JobManualActor, JobTestActor}
import asura.core.util.JacksonSupport
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class WsApi @Inject()(
                       implicit val system: ActorSystem,
                       implicit val exec: ExecutionContext,
                       implicit val mat: Materializer,
                       val controllerComponents: SecurityComponents
                     ) extends BaseApi {

  def testJob() = WebSocket.accept[String, String] { implicit req =>
    val testActor = system.actorOf(JobTestActor.props(getProfileId()))
    val incomingMessages: Sink[String, NotUsed] =
      Flow[String].map {
        case text: String => JacksonSupport.parse(text, classOf[JobTestMessage])
      }.to(Sink.actorRef[JobTestMessage](testActor, PoisonPill))
    val outgoingMessages: Source[String, NotUsed] =
      Source.actorRef[ActorEvent](100, OverflowStrategy.dropHead)
        .mapMaterializedValue { outActor =>
          testActor ! SenderMessage(outActor)
          NotUsed
        }
        .map(result => JacksonSupport.stringify(result))
        .keepAlive(2.seconds, () => "")
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  // TODO: keep alive
  def manualTestJob(jobId: String) = WebSocket.accept[String, String] { implicit req =>
    ActorFlow.actorRef { out =>
      JobManualActor.props(getProfileId(), jobId, out)
    }
  }
}
