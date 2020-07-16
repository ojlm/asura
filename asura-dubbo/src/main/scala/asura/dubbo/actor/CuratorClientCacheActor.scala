package asura.dubbo.actor

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.cache.LRUCache
import asura.common.util.StringUtils
import asura.dubbo.DubboConfig
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfacesMessage, GetProvidersMessage}
import asura.dubbo.model.{DubboInterface, DubboProvider}
import org.apache.curator.framework.api.ACLProvider
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.ACL

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class CuratorClientCacheActor extends BaseActor {

  private val lruCache = LRUCache[String, CuratorFramework](DubboConfig.DEFAULT_ZK_CLIENT_CACHE_SIZE, (_, client) => {
    client.close()
  })
  implicit val actorEC: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case GetInterfacesMessage(zkConnectString, path, zkUsername, zkPassword) =>
      getInterfaces(
        zkConnectString,
        StringUtils.notEmptyElse(path, DubboConfig.DEFAULT_ROOT_DUBBO_PATH),
        zkUsername,
        zkPassword,
      ) pipeTo sender()
    case GetProvidersMessage(zkConnectString, path, ref, zkUsername, zkPassword) =>
      getInterfaceProviders(
        zkConnectString,
        ref,
        StringUtils.notEmptyElse(path, DubboConfig.DEFAULT_ROOT_DUBBO_PATH),
        zkUsername,
        zkPassword,
      ) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def getInterfaces(
                     zkConnectString: String,
                     path: String,
                     zkUsername: String = null,
                     zkPassword: String = null,
                   ): Future[Seq[DubboInterface]] = {
    getClient(zkConnectString, path, zkUsername, zkPassword)
      .map(client => {
        client.getChildren().forPath(path).asScala
          .map(DubboInterface(zkConnectString, path, _))
          .toSeq
      })
  }

  def getInterfaceProviders(
                             zkConnectString: String,
                             ref: String,
                             path: String,
                             zkUsername: String = null,
                             zkPassword: String = null,
                           ): Future[Seq[DubboProvider]] = {
    getClient(zkConnectString, path, zkUsername, zkPassword)
      .map(client => {
        client.getChildren().forPath(s"${path}/${ref}/providers")
          .asScala
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
              zkConnectString = zkConnectString,
              path = path,
              ref = ref,
              address = uri.getHost,
              port = uri.getPort,
              methods = queryMap.getOrElse("methods", StringUtils.EMPTY).split(",").toIndexedSeq,
              application = queryMap.getOrElse("application", StringUtils.EMPTY),
              dubbo = queryMap.getOrElse("dubbo", StringUtils.EMPTY)
            )
          })
          .toSeq
      })
  }

  def getClient(
                 connectString: String,
                 path: String,
                 zkUsername: String = null,
                 zkPassword: String = null,
               ): Future[CuratorFramework] = {
    Future {
      val cacheKey = s"${connectString}/${path}"
      val client = lruCache.get(cacheKey)
      if (null == client) {
        val builder = CuratorFrameworkFactory.builder()
        builder.connectString(connectString)
          .retryPolicy(new RetryNTimes(0, 0))
        if (StringUtils.isNotEmpty(zkUsername) && StringUtils.isNotEmpty(zkPassword)) {
          builder.authorization("digest", s"${zkUsername}:${zkPassword}".getBytes)
            .aclProvider(new ACLProvider {
              override def getDefaultAcl: util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL

              override def getAclForPath(path: String): util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL
            })
        }
        val newClient = builder.build()
        newClient.start()
        lruCache.put(cacheKey, newClient)
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
