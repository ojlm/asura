package asura.dubbo.actor

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.cache.LRUCache
import asura.common.util.StringUtils
import asura.dubbo.DubboConfig
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfacesMessage, GetProvidersMessage}
import asura.dubbo.model.{DubboInterface, DubboProvider}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes

import scala.collection.{JavaConverters, mutable}
import scala.concurrent.{ExecutionContext, Future}

class CuratorClientCacheActor extends BaseActor {

  private val lruCache = LRUCache[String, CuratorFramework](DubboConfig.DEFAULT_ZK_CLIENT_CACHE_SIZE, (_, client) => {
    client.close()
  })
  implicit val actorEC: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case GetInterfacesMessage(zkAddr, path) =>
      getInterfaces(zkAddr, StringUtils.notEmptyElse(path, DubboConfig.DEFAULT_ROOT_DUBBO_PATH)) pipeTo sender()
    case GetProvidersMessage(zkAddr, path, ref) =>
      getInterfaceProviders(
        zkAddr,
        ref,
        StringUtils.notEmptyElse(path, DubboConfig.DEFAULT_ROOT_DUBBO_PATH)
      ) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def getInterfaces(zkAddr: String, path: String): Future[Seq[DubboInterface]] = {
    getClient(zkAddr)
      .map(client => {
        JavaConverters.asScalaBuffer(client.getChildren().forPath(path))
          .map(DubboInterface(zkAddr, path, _))
      })
  }

  def getInterfaceProviders(zkAddr: String, ref: String, path: String): Future[Seq[DubboProvider]] = {
    getClient(zkAddr).map(client => {
      val strings = client.getChildren().forPath(s"${path}/${ref}/providers")
      JavaConverters.asScalaBuffer(strings)
        .map(item => URLDecoder.decode(item, StandardCharsets.UTF_8.name()))
        .map(uriStr => {
          val uri = URI.create(uriStr)
          val queryMap = mutable.Map[String, String]()
          uri.getQuery.split("&").foreach(paramStr => {
            val param = paramStr.split("=")
            if (param.length == 2) {
              queryMap += (param(0) -> param(1))
            }
          })
          DubboProvider(
            zkAddr = zkAddr,
            path = path,
            ref = ref,
            address = uri.getHost,
            port = uri.getPort,
            methods = queryMap.getOrElse("methods", StringUtils.EMPTY).split(","),
            application = queryMap.getOrElse("application", StringUtils.EMPTY),
            dubbo = queryMap.getOrElse("dubbo", StringUtils.EMPTY)
          )
        })
    })
  }

  def getClient(zkAddr: String): Future[CuratorFramework] = {
    Future {
      val client = lruCache.get(zkAddr)
      if (null == client) {
        val newClient = CuratorFrameworkFactory.newClient(zkAddr, new RetryNTimes(0, 0))
        newClient.start()
        lruCache.put(zkAddr, newClient)
        newClient
      } else {
        client
      }
    }(DubboConfig.DUBBO_EC)
  }

  override def postStop(): Unit = {
    log.debug(s"Close zookeeper clients size: ${lruCache.size()} ")
    lruCache.forEach((_, client) => {
      client.close()
    })
  }
}

object CuratorClientCacheActor {
  def props() = Props(new CuratorClientCacheActor())
}
