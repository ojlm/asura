package asura.core.actor.flow

import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import asura.common.actor.ActorEvent
import asura.common.exceptions.InvalidStatusException
import asura.core.actor.messages.SenderMessage
import asura.core.util.JacksonSupport

import scala.concurrent.duration._

object WebSocketMessageHandler {

  def newHandleFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => JacksonSupport.parse(text, msgClass)
        case _ => throw InvalidStatusException("Unsupported message type")
      }.to(Sink.actorRef[T](workActor, PoisonPill))
    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[ActorEvent](100, OverflowStrategy.dropHead)
        .mapMaterializedValue { outActor =>
          workActor ! SenderMessage(outActor)
          NotUsed
        }
        .map(result => TextMessage(JacksonSupport.stringify(result)))
        .keepAlive(2.seconds, () => TextMessage.Strict(""))
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  def newHandleStringFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[Message, Message, NotUsed] = {
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => JacksonSupport.parse(text, msgClass)
        case _ => throw InvalidStatusException("Unsupported message type")
      }.to(Sink.actorRef[T](workActor, PoisonPill))
    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[String](100, OverflowStrategy.dropHead)
        .mapMaterializedValue { outActor =>
          workActor ! SenderMessage(outActor)
          NotUsed
        }
        .map(result => TextMessage(result))
        .keepAlive(2.seconds, () => TextMessage.Strict(""))
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
