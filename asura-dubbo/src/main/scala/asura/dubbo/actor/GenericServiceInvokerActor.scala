package asura.dubbo.actor

import akka.actor.{Props, Status}
import akka.pattern.pipe
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.common.util.{LogUtils, StringUtils}
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfaceMethodParams, GetInterfacesMessage, GetProvidersMessage}
import asura.dubbo.cache.{CuratorClientCache, ReferenceCache}
import asura.dubbo.model.{DubboInterface, DubboProvider}
import asura.dubbo.{DubboConfig, GenericRequest}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class GenericServiceInvokerActor extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 30.seconds

  override def receive: Receive = {
    case GetInterfacesMessage(zkAddr, path) =>
      getInterfaces(zkAddr, path) pipeTo sender()
    case GetProvidersMessage(zkAddr, path, ref) =>
      getProviders(zkAddr, path, ref) pipeTo sender()
    case request: GenericRequest =>
      test(request) pipeTo sender()
    case msg: GetInterfaceMethodParams =>
      context.actorOf(InterfaceMethodParamsActor.props(sender(), msg))
    case Status.Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
      Future.failed(t) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def getInterfaces(zkAddr: String, path: String): Future[Seq[DubboInterface]] = {
    val dubboPath = if (StringUtils.isEmpty(path)) {
      DubboConfig.DEFAULT_ROOT_DUBBO_PATH
    } else {
      path
    }
    CuratorClientCache.getChildren(zkAddr, dubboPath).map(items => {
      items.map(DubboInterface(zkAddr, dubboPath, _))
    })
  }

  def getProviders(zkAddr: String, path: String, ref: String): Future[Seq[DubboProvider]] = {
    CuratorClientCache.getInterfaceProviders(zkAddr, ref)
  }

  def test(request: GenericRequest): Future[Any] = {
    // TODO: clear cache when timeout
    val (service, refConfig) = ReferenceCache.getServiceAndConfig(request)
    Future {
      // https://github.com/apache/incubator-dubbo/issues/3163
      service.$invoke(request.method, request.getParameterTypes(), request.getArgs())
    }(ExecutionContext.global)
  }

  override def postStop(): Unit = {
    CuratorClientCache.destroy()
  }
}

object GenericServiceInvokerActor {

  def props() = Props(new GenericServiceInvokerActor())

  case class GetInterfacesMessage(zkAddr: String, path: String)

  case class GetProvidersMessage(zkAddr: String, path: String, ref: String)

  case class GetInterfaceMethodParams(address: String, port: Int, ref: String)

}
