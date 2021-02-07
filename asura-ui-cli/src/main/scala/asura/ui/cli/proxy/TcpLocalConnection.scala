package asura.ui.cli.proxy

import akka.actor.{ActorRef, Props, Stash, Terminated}
import akka.io.Tcp
import asura.common.actor.BaseActor
import asura.ui.cli.proxy.TcpProxy.{LocalConnectionClosedMessage, LocalDataMessage, RemoteConnectionClosedMessage, RemoteDataMessage}

class TcpLocalConnection(
                          connection: ActorRef,
                          config: TcpProxyConfig,
                        ) extends BaseActor with Stash {

  val remoteConnection = context.actorOf(TcpRemoteConnection.props(config))
  context watch remoteConnection

  override def receive: Receive = {
    case Tcp.Received(data) =>
      remoteConnection ! LocalDataMessage(data)
    case RemoteDataMessage(data) =>
      connection ! Tcp.Write(data, TcpLocalConnection.Ack)
      context.become(waitingForAck, discardOld = false)
    case Tcp.CommandFailed(cmd: Tcp.Write) =>
      log.warning(s"write failed: ${cmd.failureMessage}")
      connection ! Tcp.ResumeWriting
    case Tcp.CommandFailed(cmd) =>
      log.error(s"command failed: ${cmd.failureMessage}")
      remoteConnection ! LocalConnectionClosedMessage
    case RemoteConnectionClosedMessage =>
      connection ! Tcp.ConfirmedClose
    case _: Tcp.ConnectionClosed =>
      remoteConnection ! LocalConnectionClosedMessage
      context become disconnected
    case Terminated(remoteConnection) =>
      connection ! Tcp.Close
      context stop self
  }

  def disconnected: Receive = {
    case Terminated(remoteConnection) =>
      context stop self
    case RemoteConnectionClosedMessage =>
  }

  def waitingForAck: Receive = {
    case TcpLocalConnection.Ack =>
      unstashAll()
      context.unbecome()
    case _ => stash()
  }

}

object TcpLocalConnection {

  def props(connection: ActorRef, config: TcpProxyConfig) = Props(new TcpLocalConnection(connection, config))

  object Ack extends Tcp.Event

}
