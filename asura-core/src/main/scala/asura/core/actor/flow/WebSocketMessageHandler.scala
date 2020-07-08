package asura.core.actor.flow

import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill, Status}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import asura.common.actor.{ActorEvent, SenderMessage}
import asura.common.exceptions.InvalidStatusException
import asura.core.CoreConfig
import asura.core.util.JacksonSupport

import scala.concurrent.duration._

object WebSocketMessageHandler {

  val DEFAULT_BUFFER_SIZE = CoreConfig.DEFAULT_WS_ACTOR_BUFFER_SIZE
  val KEEP_ALIVE_INTERVAL = 2
  val completionMatcher: PartialFunction[Any, CompletionStrategy] = {
    case akka.actor.Status.Success => CompletionStrategy.draining
  }

  def newHandleFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => JacksonSupport.parse(text, msgClass)
        case _ => throw InvalidStatusException("Unsupported message type")
      }.to(Sink.actorRef(workActor, PoisonPill, t => Status.Failure(t)))
    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[ActorEvent](
        completionMatcher,
        PartialFunction.empty,
        DEFAULT_BUFFER_SIZE,
        OverflowStrategy.dropHead
      ).mapMaterializedValue { outActor =>
        workActor ! SenderMessage(outActor)
        NotUsed
      }.map(result => TextMessage(JacksonSupport.stringify(result)))
        .keepAlive(KEEP_ALIVE_INTERVAL.seconds, () => TextMessage.Strict(""))
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  def newHandleStringFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => JacksonSupport.parse(text, msgClass)
        case _ => throw InvalidStatusException("Unsupported message type")
      }.to(Sink.actorRef(workActor, PoisonPill, t => Status.Failure(t)))
    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[String](
        completionMatcher,
        PartialFunction.empty,
        DEFAULT_BUFFER_SIZE, OverflowStrategy.dropHead
      ).mapMaterializedValue { outActor =>
        workActor ! SenderMessage(outActor)
        NotUsed
      }.map(result => TextMessage(result))
        .keepAlive(KEEP_ALIVE_INTERVAL.seconds, () => TextMessage.Strict(""))
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  def stringToActorEventFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[String, String, NotUsed] = {
    val incomingMessages: Sink[String, NotUsed] =
      Flow[String].map {
        case text: String => JacksonSupport.parse(text, msgClass)
      }.to(Sink.actorRef(workActor, PoisonPill, t => Status.Failure(t)))
    val outgoingMessages: Source[String, NotUsed] =
      Source.actorRef[ActorEvent](
        completionMatcher,
        PartialFunction.empty,
        DEFAULT_BUFFER_SIZE, OverflowStrategy.dropHead
      ).mapMaterializedValue { outActor =>
        workActor ! SenderMessage(outActor)
        NotUsed
      }.map(result => JacksonSupport.stringify(result))
        .keepAlive(KEEP_ALIVE_INTERVAL.seconds, () => "")
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  def stringToActorEventFlow[T <: AnyRef](workActor: ActorRef): Flow[String, String, NotUsed] = {
    val incomingMessages: Sink[String, NotUsed] =
      Flow[String].to(Sink.actorRef(workActor, PoisonPill, t => Status.Failure(t)))
    val outgoingMessages: Source[String, NotUsed] =
      Source.actorRef[String](
        completionMatcher,
        PartialFunction.empty,
        DEFAULT_BUFFER_SIZE, OverflowStrategy.dropHead
      ).mapMaterializedValue { outActor =>
        workActor ! SenderMessage(outActor)
        NotUsed
      }.map(result => JacksonSupport.stringify(result))
        .keepAlive(KEEP_ALIVE_INTERVAL.seconds, () => "")
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
