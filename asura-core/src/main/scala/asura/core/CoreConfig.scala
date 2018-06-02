package asura.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.common.util.StringUtils
import asura.core.es.EsClient
import asura.core.redis.RedisClient

import scala.concurrent.ExecutionContext

case class CoreConfig(
                       val system: ActorSystem,
                       val dispatcher: ExecutionContext,
                       val materializer: ActorMaterializer,
                       val redisServers: Seq[String],
                       val esUrl: String,
                       val proxyIdentifier: String,
                       val proxyHost: String = StringUtils.EMPTY, /* for https transparent proxy */
                       val httpProxyPort: Int = 0,
                       val httpsProxyPort: Int = 0,
                       val reportBaseUrl: String = StringUtils.EMPTY
                     )

object CoreConfig {

  implicit var system: ActorSystem = null
  implicit var dispatcher: ExecutionContext = null
  implicit var materializer: ActorMaterializer = null
  var proxyHost: String = null
  var httpProxyPort: Int = 0
  var httpsProxyPort: Int = 0
  var proxyIdentifier: String = null
  var reportBaseUrl: String = StringUtils.EMPTY

  def init(config: CoreConfig): Unit = {
    system = config.system
    dispatcher = config.dispatcher
    materializer = config.materializer
    RedisClient.init(config.redisServers)
    EsClient.init(config.esUrl)
    CoreConfig.proxyHost = config.proxyHost
    CoreConfig.httpProxyPort = config.httpProxyPort
    CoreConfig.httpsProxyPort = config.httpsProxyPort
    CoreConfig.proxyIdentifier = config.proxyIdentifier
    CoreConfig.reportBaseUrl = config.reportBaseUrl
  }
}
