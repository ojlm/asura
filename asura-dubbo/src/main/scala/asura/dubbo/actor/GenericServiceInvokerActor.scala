package asura.dubbo.actor

import akka.actor.{Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfaceMethodParams, GetInterfacesMessage, GetProvidersMessage}
import asura.dubbo.{DubboConfig, GenericRequest}

import scala.concurrent.{ExecutionContext, Future}

class GenericServiceInvokerActor extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = DubboConfig.DEFAULT_ACTOR_ASK_TIMEOUT

  val curatorClientCacheActor = context.actorOf(CuratorClientCacheActor.props())
  val referenceActor = context.actorOf(DubboReferenceCacheActor.props())

  override def receive: Receive = {
    case msg: GetInterfacesMessage =>
      curatorClientCacheActor ? msg pipeTo sender()
    case msg: GetProvidersMessage =>
      curatorClientCacheActor ? msg pipeTo sender()
    case msg: GenericRequest =>
      referenceActor ? msg pipeTo sender()
    case msg: GetInterfaceMethodParams =>
      context.actorOf(InterfaceMethodParamsActor.props(sender(), msg))
    case Status.Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
      Future.failed(t) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  override def postStop(): Unit = {
    DubboConfig.DUBBO_EC.shutdown()
  }
}

object GenericServiceInvokerActor {

  def props() = Props(new GenericServiceInvokerActor())

  case class GetInterfacesMessage(zkConnectString: String, path: String)

  case class GetProvidersMessage(zkConnectString: String, path: String, ref: String)

  case class GetInterfaceMethodParams(address: String, port: Int, ref: String)

}
