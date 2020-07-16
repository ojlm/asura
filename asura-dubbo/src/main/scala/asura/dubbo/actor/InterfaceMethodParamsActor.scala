package asura.dubbo.actor

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.pipe
import akka.util.ByteString
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.dubbo.DubboConfig
import asura.dubbo.actor.GenericServiceInvokerActor.GetInterfaceMethodParams
import asura.dubbo.model.InterfaceMethodParams
import asura.dubbo.model.InterfaceMethodParams.MethodSignature

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

class InterfaceMethodParamsActor(invoker: ActorRef, msg: GetInterfaceMethodParams) extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  private val telnet: ActorRef = context.actorOf(TelnetClientActor.props(msg.address, if (msg.port > 0) msg.port else DubboConfig.DEFAULT_PORT, self))

  override def receive: Receive = {
    case telnetData: ByteString =>
      val utf8String = telnetData.utf8String
      if (utf8String.contains(TelnetClientActor.MSG_CONNECT_TO)) {
        log.debug(utf8String)
        if (utf8String.contains(TelnetClientActor.MSG_SUCCESS)) {
          telnet ! ByteString(s"ls -l ${msg.ref}\r\n")
        } else if (utf8String.contains(TelnetClientActor.MSG_FAIL)) {
          Future.failed(new RuntimeException(s"Remote connection to ${msg.address}:${msg.port} failed")) pipeTo invoker
          telnet ! TelnetClientActor.CMD_CLOSE
          context stop self
        } else {
          Future.failed(new RuntimeException(s"Unknown response ${utf8String}")) pipeTo invoker
          telnet ! TelnetClientActor.CMD_CLOSE
          context stop self
        }
      } else if (utf8String.contains("(") && utf8String.contains(")")) {
        getInterfaceMethodParams(msg.ref, utf8String) pipeTo invoker
        telnet ! TelnetClientActor.CMD_CLOSE
      } else {
        Future.failed(new RuntimeException(s"Unknown response: ${utf8String}")) pipeTo invoker
        telnet ! TelnetClientActor.CMD_CLOSE
        context stop self
      }
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      context stop self
  }

  def getInterfaceMethodParams(ref: String, content: String): Future[InterfaceMethodParams] = {
    Future.successful {
      val methods = ArrayBuffer[MethodSignature]()
      content.split("\r\n")
        .filter(!_.startsWith(DubboConfig.DEFAULT_PROMPT))
        .map(signature => {
          val splits = signature.split(" ")
          if (splits.length == 2) {
            val ret = splits(0)
            val secondPart = splits(1)
            val idx = secondPart.indexOf("(")
            val method = secondPart.substring(0, idx)
            val params = secondPart.substring(idx + 1, secondPart.length - 1).split(",").toIndexedSeq
            methods += (MethodSignature(ret, method, params))
          }
        })
      InterfaceMethodParams(ref, methods.toSeq)
    }
  }

  override def postStop(): Unit = log.debug(s"${self.path} stopped")
}

object InterfaceMethodParamsActor {
  def props(invoker: ActorRef, msg: GetInterfaceMethodParams) = {
    Props(new InterfaceMethodParamsActor(invoker, msg))
  }
}


