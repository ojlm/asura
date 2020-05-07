package asura.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import asura.common.util.StringUtils
import asura.core.CoreConfig.{EsOnlineLogConfig, LinkerdConfig}
import asura.core.es.{EsClient, EsConfig}
import com.sksamuel.elastic4s.ElasticClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class CoreConfig(
                       system: ActorSystem,
                       dispatcher: ExecutionContext,
                       materializer: Materializer,
                       redisServers: Seq[String],
                       esIndexPrefix: Option[String] = None,
                       esUrl: String,
                       linkerdConfig: LinkerdConfig,
                       reportBaseUrl: String = StringUtils.EMPTY,
                       onlineConfigs: Seq[EsOnlineLogConfig] = Nil,
                       securityConfig: SecurityConfig = SecurityConfig(),
                     )

object CoreConfig {

  val DEFAULT_WS_ACTOR_BUFFER_SIZE = 10000
  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes
  implicit val DEFAULT_JOB_TIMEOUT: Timeout = 30.minutes
  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: Materializer = _
  var reportBaseUrl: String = StringUtils.EMPTY
  var linkerdConfig: LinkerdConfig = _
  var securityConfig: SecurityConfig = _

  def init(config: CoreConfig): Unit = {
    system = config.system
    dispatcher = config.dispatcher
    materializer = config.materializer
    CoreConfig.securityConfig = config.securityConfig
    // RedisClient.init(config.redisServers)
    CoreConfig.linkerdConfig = config.linkerdConfig
    CoreConfig.reportBaseUrl = config.reportBaseUrl
    if (config.esIndexPrefix.nonEmpty) {
      EsConfig.IndexPrefix = config.esIndexPrefix.get
    }
    EsClient.init(config.esUrl)
    EsClient.initOnlineLogClient(config.onlineConfigs)
    RunnerActors.init(system)
  }

  case class EsOnlineLogConfig(
                                tag: String,
                                url: String,
                                prefix: String,
                                datePattern: String,
                                fieldDomain: String,
                                fieldMethod: String,
                                fieldUri: String,
                                fieldRequestTime: String,
                                fieldRequestTimeResolution: Double,
                                fieldRemoteAddr: String = null,
                                excludeRemoteAddrs: Seq[String] = Nil,
                              ) {
    var onlineLogClient: ElasticClient = _
  }

  case class LinkerdConfig(
                            enabled: Boolean,
                            servers: Seq[LinkerdConfigServer]
                          )

  case class LinkerdConfigServer(
                                  tag: String,
                                  description: String,
                                  namerd: String,
                                  proxyHost: String,
                                  httpProxyPort: Int,
                                  httpsProxyPort: Int,
                                  headerIdentifier: String,
                                  httpNs: String,
                                )

}
