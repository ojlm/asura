package asura.dubbo.actor

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props, Status}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.dubbo.DubboConfig

class TelnetClientActor(remote: InetSocketAddress, listener: ActorRef) extends BaseActor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      listener ! ByteString(s"${TelnetClientActor.MSG_CONNECT_TO} ${remote.getAddress.getHostAddress}:${remote.getPort} ${TelnetClientActor.MSG_FAIL}\r\n")
    case Connected(remote, local) =>
      log.debug(s"local address: ${local}, remote address: ${remote}")
      listener ! ByteString(s"${TelnetClientActor.MSG_CONNECT_TO} ${remote.getAddress.getHostAddress}:${remote.getPort} ${TelnetClientActor.MSG_SUCCESS}\r\n")
      val remoteConnection = sender()
      remoteConnection ! Register(self)
      context.become {
        case data: ByteString =>
          remoteConnection ! Write(data)
        case CommandFailed(_: Write) =>
          listener ! ByteString("write failed\r\n")
        case Received(data) =>
          listener ! data
        case TelnetClientActor.CMD_CLOSE =>
          remoteConnection ! Close
        case _: ConnectionClosed =>
          listener ! ByteString(s"connection to ${remote.getAddress.getHostAddress}:${remote.getPort} closed\r\n")
          context stop self
      }
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      listener ! t.getMessage
      context stop self
  }
}


object TelnetClientActor {

  val CMD_CLOSE = "close"
  val MSG_CONNECT_TO = "connect to"
  val MSG_SUCCESS = "success"
  val MSG_FAIL = "fail"

  def props(remote: InetSocketAddress, replies: ActorRef) = {
    Props(new TelnetClientActor(remote, replies))
  }

  def props(address: String, port: Int, replies: ActorRef) = {
    Props(
      new TelnetClientActor(
        new InetSocketAddress(address, if (port > 0) port else DubboConfig.DEFAULT_PORT),
        replies
      )
    )
  }
}
