package asura.app.api.ci

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import asura.common.actor.SenderMessage
import asura.core.actor.flow.WebSocketMessageHandler.completionMatcher
import asura.core.ci.{CiManager, CiTriggerEventMessage}
import asura.core.job.actor.JobCiActor
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Codec, WebSocket}

import scala.concurrent.{ExecutionContext, Future}

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
}
