package asura.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import asura.common.util.StringUtils
import asura.core.CoreConfig.{EsOnlineLogConfig, LinkerdConfig}
import asura.core.es.{EsClient, EsConfig}
import com.sksamuel.elastic4s.http.ElasticClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class CoreConfig(
                       val system: ActorSystem,
                       val dispatcher: ExecutionContext,
                       val materializer: ActorMaterializer,
                       val redisServers: Seq[String],
                       val esIndexPrefix: Option[String] = None,
                       val esUrl: String,
                       val linkerdConfig: LinkerdConfig,
                       val useLocalEsNode: Boolean = true,
                       val localEsDataDir: String = StringUtils.EMPTY,
                       val reportBaseUrl: String = StringUtils.EMPTY,
                       val onlineConfigs: Seq[EsOnlineLogConfig] = Nil,
                       val securityConfig: SecurityConfig = SecurityConfig(),
                     )

object CoreConfig {

  val DEFAULT_WS_ACTOR_BUFFER_SIZE = 10000
  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes
  implicit val DEFAULT_JOB_TIMEOUT: Timeout = 30.minutes
  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: ActorMaterializer = _
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
    EsClient.init(config.useLocalEsNode, config.esUrl, config.localEsDataDir)
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
