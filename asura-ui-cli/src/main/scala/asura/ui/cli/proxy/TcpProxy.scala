package asura.ui.cli.proxy

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import asura.common.actor.BaseActor

class TcpProxy(config: TcpProxyConfig) extends BaseActor {

  IO(Tcp)(context.system) ! Tcp.Bind(self, config.local)

  override def receive: Receive = {
    case Tcp.Bound(local) =>
      log.info(s"listening on: ${local.toString}")
      context become bound
    case Tcp.CommandFailed(cmd) =>
      log.error(s"error binding to socket: ${cmd.failureMessage}")
      context stop self
  }

  def bound: Receive = {
    case Tcp.Connected(remote, _) =>
      log.info(s"connection from ${remote.toString}")
      sender() ! Tcp.Register(createConnectionHandler(sender(), remote))
    case Tcp.CommandFailed(cmd) =>
      log.error(s"command failed: ${cmd.failureMessage}")
      context stop self
  }

  def createConnectionHandler(connection: ActorRef, remote: InetSocketAddress) = {
    context.actorOf(TcpLocalConnection.props(connection, config), s"local-${remote.getHostName}-${remote.getPort}")
  }

}

object TcpProxy {

  def props(config: TcpProxyConfig) = Props(new TcpProxy(config))

  case class LocalDataMessage(data: ByteString)

  case object LocalConnectionClosedMessage

  case class RemoteDataMessage(data: ByteString)

  case object RemoteConnectionClosedMessage

}
