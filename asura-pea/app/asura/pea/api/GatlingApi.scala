package asura.pea.api

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import asura.common.actor.{ActorEvent, SenderMessage}
import asura.common.util.{JsonUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaManagerActor.{GetNodeStatusMessage, SingleHttpScenarioMessage}
import asura.pea.actor.PeaWebMonitorActor
import asura.pea.actor.PeaWebMonitorActor.WebMonitorController
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.WebSocket

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatlingApi @Inject()(
                            implicit val system: ActorSystem,
                            implicit val exec: ExecutionContext,
                            implicit val mat: Materializer,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi {

  val DEFAULT_BUFFER_SIZE = 10000
  val KEEP_ALIVE_INTERVAL = 2
  val peaManager = PeaConfig.managerActor

  def status() = Action.async { implicit req =>
    (peaManager ? GetNodeStatusMessage).toOkResult
  }

  def single() = Action(parse.byteString).async { implicit req =>
    val message = req.bodyAs(classOf[SingleHttpScenarioMessage])
    (peaManager ? message).toOkResult
  }

  def monitor() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      Right {
        val actorRef = system.actorOf(PeaWebMonitorActor.props())
        stringToActorEventFlow(actorRef, classOf[WebMonitorController])
      }
    }
  }

  def stringToActorEventFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[String, String, NotUsed] = {
    val incomingMessages: Sink[String, NotUsed] =
      Flow[String].map {
        case text: String => JsonUtils.parse(text, msgClass)
      }.to(Sink.actorRef[T](workActor, PoisonPill))
    val outgoingMessages: Source[String, NotUsed] =
      Source.actorRef[ActorEvent](DEFAULT_BUFFER_SIZE, OverflowStrategy.dropHead)
        .mapMaterializedValue { outActor =>
          workActor ! SenderMessage(outActor)
          NotUsed
        }
        .map(result => JsonUtils.stringify(result))
        .keepAlive(KEEP_ALIVE_INTERVAL.seconds, () => StringUtils.EMPTY)
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
