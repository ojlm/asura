package asura.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.common.util.StringUtils
import asura.core.es.{EsClient, EsConfig}

import scala.concurrent.ExecutionContext

case class CoreConfig(
                       val system: ActorSystem,
                       val dispatcher: ExecutionContext,
                       val materializer: ActorMaterializer,
                       val redisServers: Seq[String],
                       val esIndexPrefix: Option[String] = None,
                       val esUrl: String,
                       val proxyIdentifier: String,
                       val useLocalEsNode: Boolean = true,
                       val localEsDataDir: String = StringUtils.EMPTY,
                       val enableLinkerd: Boolean = false,
                       val proxyHost: String = StringUtils.EMPTY, /* for https transparent proxy */
                       val httpProxyPort: Int = 0,
                       val httpsProxyPort: Int = 0,
                       val reportBaseUrl: String = StringUtils.EMPTY
                     )

object CoreConfig {

  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: ActorMaterializer = _
  var proxyHost: String = _
  var httpProxyPort: Int = 0
  var httpsProxyPort: Int = 0
  var proxyIdentifier: String = _
  var reportBaseUrl: String = StringUtils.EMPTY

  def init(config: CoreConfig): Unit = {
    system = config.system
    dispatcher = config.dispatcher
    materializer = config.materializer
    // RedisClient.init(config.redisServers)
    EsClient.init(config.useLocalEsNode, config.esUrl, config.localEsDataDir)
    CoreConfig.proxyHost = config.proxyHost
    CoreConfig.httpProxyPort = config.httpProxyPort
    CoreConfig.httpsProxyPort = config.httpsProxyPort
    CoreConfig.proxyIdentifier = config.proxyIdentifier
    CoreConfig.reportBaseUrl = config.reportBaseUrl
    if (config.esIndexPrefix.nonEmpty) {
      EsConfig.IndexPrefix = config.esIndexPrefix.get
    }
  }
}
