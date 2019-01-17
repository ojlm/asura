package asura.dubbo.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.util.ByteString
import asura.common.actor.{BaseActor, ErrorActorEvent, NotifyActorEvent, SenderMessage}
import asura.common.util.LogUtils

class TelnetDubboProviderActor(address: String, port: Int) extends BaseActor {

  override def receive: Receive = {
    case SenderMessage(sender) =>
      val providerActor = context.actorOf(TelnetClientActor.props(address, port, self))
      context.become(handleRequest(sender, providerActor))
  }

  def handleRequest(wsActor: ActorRef, providerActor: ActorRef): Receive = {
    case cmd: String =>
      if (cmd == TelnetDubboProviderActor.CMD_EXIT || cmd == TelnetDubboProviderActor.CMD_QUIT) {
        providerActor ! ByteString(TelnetClientActor.CMD_CLOSE)
        wsActor ! NotifyActorEvent(TelnetDubboProviderActor.MSG_BYE)
      } else {
        providerActor ! ByteString(cmd)
      }
    case data: ByteString =>
      wsActor ! NotifyActorEvent(data.utf8String)
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      wsActor ! ErrorActorEvent(t.getMessage)
      providerActor ! ByteString(TelnetClientActor.CMD_CLOSE)
      wsActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${address}:${port} stopped")
  }
}

object TelnetDubboProviderActor {

  val CMD_QUIT = "quit"
  val CMD_EXIT = "exit"
  val MSG_BYE = "Bye!"

  def props(address: String, port: Int) = Props(new TelnetDubboProviderActor(address, port))
}
