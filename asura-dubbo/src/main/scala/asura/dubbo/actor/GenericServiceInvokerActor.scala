package asura.dubbo.actor

import akka.actor.{Props, Status}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{LogUtils, StringUtils}
import asura.dubbo.DubboConfig
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfacesMessage, GetProvidersMessage}
import asura.dubbo.model.{DubboInterface, DubboProvider}

import scala.concurrent.{ExecutionContext, Future}

class GenericServiceInvokerActor extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case GetInterfacesMessage(zkAddr, path) =>
      getInterfaces(zkAddr, path) pipeTo sender()
    case GetProvidersMessage(zkAddr, path, ref) =>
      getProviders(zkAddr, path, ref) pipeTo sender()
    case Status.Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
    case _ =>
  }

  def getInterfaces(zkAddr: String, path: String): Future[Seq[DubboInterface]] = {
    // TODO 1. conn zk 2. get path
    val dubboPath = if (StringUtils.isNotEmpty(path)) {
      DubboConfig.DEFAULT_ROOT_DUBBO_PATH
    } else {
      path
    }
    Future.successful {
      1.to(30).map(_ => DubboInterface(zkAddr, dubboPath, this.getClass.getName))
    }
  }

  def getProviders(zkAddr: String, path: String, ref: String): Future[Seq[DubboProvider]] = {
    Future.successful {
      1.to(30).map(_ => DubboProvider(zkAddr, path, ref, "127.0.0.1", 20880, Seq("getInterfaces", "getProviders")))
    }
  }

}

object GenericServiceInvokerActor {

  def props() = Props(new GenericServiceInvokerActor())

  case class GetInterfacesMessage(zkAddr: String, path: String)

  case class GetProvidersMessage(zkAddr: String, path: String, ref: String)

}
