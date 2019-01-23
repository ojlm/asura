package asura.dubbo.cache

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

import asura.common.util.StringUtils
import asura.dubbo.DubboConfig
import asura.dubbo.model.DubboProvider
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes

import scala.collection.{JavaConverters, mutable}
import scala.concurrent.{ExecutionContext, Future}

object CuratorClientCache {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  def getChildren(zkAddr: String, path: String = DubboConfig.DEFAULT_ROOT_DUBBO_PATH): Future[Seq[String]] = {
    Future {
      val curator = CuratorFrameworkFactory.newClient(zkAddr, new RetryNTimes(0, 0))
      curator.start()
      val strings = curator.getChildren().forPath(path)
      JavaConverters.asScalaBuffer(strings)
    }
  }

  def getInterfaceProviders(zkAddr: String, ref: String, path: String = DubboConfig.DEFAULT_ROOT_DUBBO_PATH): Future[Seq[DubboProvider]] = {
    Future {
      val curator = CuratorFrameworkFactory.newClient(zkAddr, new RetryNTimes(0, 0))
      curator.start()
      val strings = curator.getChildren().forPath(s"${path}/${ref}/providers")
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
    }
  }

  def destroy(): Unit = {
    ec.shutdown()
  }
}
