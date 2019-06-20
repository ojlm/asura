package asura.pea.hook

import java.net.{InetAddress, NetworkInterface, URLEncoder}
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.common.util.LogUtils
import asura.pea.PeaConfig
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.collection.JavaConverters._
import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration,
                                ) extends StrictLogging {

  PeaConfig.system = system
  PeaConfig.dispatcher = system.dispatcher
  PeaConfig.materializer = ActorMaterializer()(system)
  registerToZK()

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      if (null != PeaConfig.zkClient) PeaConfig.zkClient.close()
    }(system.dispatcher)
  }

  def registerToZK(): Unit = {
    val addressOpt = configuration.getOptional[String]("pea.address")
    val address = if (addressOpt.nonEmpty) {
      addressOpt.get
    } else {
      val enumeration = NetworkInterface.getNetworkInterfaces.asScala.toSeq
      val ipAddresses = enumeration.flatMap(p =>
        p.getInetAddresses.asScala.toSeq
      )
      val address = ipAddresses.find { address =>
        val host = address.getHostAddress
        host.contains(".") && !address.isLoopbackAddress
      }.getOrElse(InetAddress.getLocalHost)
      address.getHostAddress
    }
    val portOpt = configuration.getOptional[Int]("pea.port")
    val hostname = try {
      import scala.sys.process._
      "hostname".!!.trim
    } catch {
      case _: Throwable => "Unknown"
    }
    val uri = s"pea://${address}:${portOpt.getOrElse(9000)}?hostname=${hostname}"
    PeaConfig.zkPath = configuration.getOptional[String]("pea.zk.path").get
    val connectString = configuration.get[String]("pea.zk.connectString")
    PeaConfig.zkClient = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1000, 3))
    PeaConfig.zkClient.start()
    try {
      PeaConfig.zkClient.create()
        .creatingParentsIfNeeded()
        .withMode(CreateMode.EPHEMERAL)
        .forPath(s"${PeaConfig.zkPath}/${URLEncoder.encode(uri, StandardCharsets.UTF_8.name())}", null)
    } catch {
      case t: Throwable => logger.warn(LogUtils.stackTraceToString(t))
    }
  }
}
