package asura.ui.cli.proxy

import akka.actor.{ActorRef, Props, Stash}
import akka.io.{IO, Tcp}
import asura.common.actor.BaseActor
import asura.ui.cli.proxy.TcpProxy.{LocalConnectionClosedMessage, LocalDataMessage, RemoteConnectionClosedMessage, RemoteDataMessage}

class TcpRemoteConnection(config: TcpProxyConfig) extends BaseActor with Stash {

  IO(Tcp)(context.system) ! Tcp.Connect(config.remote)

  override def receive: Receive = {
    case Tcp.Connected(remote, _) =>
      log.info(s"connected to: ${remote.toString}")
      sender() ! Tcp.Register(self)
      unstashAll()
      context.become(connected(sender()))
    case Tcp.CommandFailed(cmd) =>
      log.error(s"command failed: ${cmd.failureMessage}")
      context stop self
    case LocalConnectionClosedMessage =>
      context stop self
    case _ => stash()
  }

  def connected(connection: ActorRef): Receive = {
    case Tcp.Received(data) =>
      context.parent ! RemoteDataMessage(data)
    case LocalDataMessage(data) =>
      connection ! Tcp.Write(data, TcpRemoteConnection.Ack)
      context.become(waitingForAck, discardOld = false)
    case Tcp.CommandFailed(cmd) =>
      log.error(s"command failed: ${cmd.failureMessage}")
      context stop self
    case LocalConnectionClosedMessage =>
      connection ! Tcp.Close
    case _: Tcp.ConnectionClosed =>
      context.parent ! RemoteConnectionClosedMessage
      context stop self
  }

  def waitingForAck: Receive = {
    case TcpRemoteConnection.Ack =>
      unstashAll()
      context.unbecome()
    case _ => stash()
  }

}

object TcpRemoteConnection {

  def props(config: TcpProxyConfig) = Props(new TcpRemoteConnection(config))

  object Ack extends Tcp.Event

}
