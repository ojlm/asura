package asura.app.hook

import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.app.api.auth.{BasicAuth, Reserved}
import asura.app.notify.MailNotifier
import asura.app.store.FileSystemBasedEngine
import asura.cluster.ClusterManager
import asura.common.util.{HostUtils, LogUtils, StringUtils}
import asura.core.CoreConfig.{EsOnlineLogConfig, LinkerdConfig, LinkerdConfigServer}
import asura.core.auth.AuthManager
import asura.core.ci.CiManager
import asura.core.concurrent.ExecutionContextManager
import asura.core.es.EsClient
import asura.core.es.actor.UiTaskListenerActor
import asura.core.job.JobCenter
import asura.core.job.actor.SchedulerActor
import asura.core.notify.JobNotifyManager
import asura.core.store.BlobStoreEngines
import asura.core.{CoreConfig, SecurityConfig}
import asura.namerd.NamerdConfig
import asura.ui.UiConfig
import asura.ui.driver.UiDriverProvider
import asura.ui.model.ChromeDriverInfo
import com.typesafe.config.{ConfigFactory, ConfigList}
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.mailer.MailerClient

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration,
                                  uiDriverProvider: UiDriverProvider,
                                  mailerClient: MailerClient
                                ) {
  val logger = LoggerFactory.getLogger(classOf[ApplicationStart])
  logger.info("application started")
  val config = configuration.underlying
  // init other modules
  if (configuration.getOptional[Boolean]("asura.job.enabled").getOrElse(false)) {
    logger.info("init job modules")
    JobCenter.init(configuration.get[String]("asura.job.workDir"), configuration.get[String]("asura.reportBaseUrl"))
    val defaultSchedulerConfig = config.getConfig("asura.job.default")
    val systemSchedulerConfig = config.getConfig("asura.job.system")
    val quartzCommon = config.getConfig("asura.job.quartz")
    import asura.common.config.PropertiesConversions.toProperties
    system.actorOf(SchedulerActor.props(toProperties(quartzCommon, defaultSchedulerConfig), toProperties(quartzCommon, systemSchedulerConfig)), "JobScheduler")
  }
  private val materializer = Materializer(system)
  CoreConfig.init(CoreConfig(
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer,
    redisServers = Nil,
    esIndexPrefix = configuration.getOptional[String]("asura.es.indexPrefix"),
    esUrl = configuration.get[String]("asura.es.url"),
    esUsername = configuration.getOptional[String]("asura.es.username").getOrElse(null),
    esPassword = configuration.getOptional[String]("asura.es.password").getOrElse(null),
    linkerdConfig = toLinkerdConfig(configuration),
    reportBaseUrl = configuration.getOptional[String]("asura.reportBaseUrl").getOrElse(""),
    onlineConfigs = toEsOnlineConfigs(configuration.getOptional[ConfigList]("asura.es.onlineLog")),
    securityConfig = toSecurityConfig(configuration)
  ))
  NamerdConfig.init(
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer
  )

  // reserved
  Reserved.initReservedData()

  // add auth
  AuthManager.register(BasicAuth)

  // CI Events
  CiManager.init(system)

  // add notify
  JobNotifyManager.register(MailNotifier(mailerClient))

  // blobstore
  BlobStoreEngines.register(FileSystemBasedEngine(configuration))

  // ui
  if (configuration.getOptional[Boolean]("asura.ui.enabled").getOrElse(false)) {
    val syncInterval = configuration.getOptional[Int]("asura.ui.sync.interval").getOrElse(30)
    val host = configuration.getOptional[String]("asura.ui.proxy.host").getOrElse(StringUtils.EMPTY)
    val port = configuration.getOptional[Int]("asura.ui.proxy.port").getOrElse(0)
    val password = configuration.getOptional[String]("asura.ui.proxy.password").getOrElse(StringUtils.EMPTY)
    val localChrome = ChromeDriverInfo(host, port, password)
    localChrome.hostname = HostUtils.hostname
    UiConfig.init(UiConfig(
      system,
      ExecutionContextManager.cachedExecutor,
      system.actorOf(UiTaskListenerActor.props()),
      localChrome = localChrome,
      uiDriverProvider = uiDriverProvider,
      syncInterval,
    ))
  }

  if (configuration.getOptional[Boolean]("asura.cluster.enabled").getOrElse(false)) {
    val hostnameOpt = configuration.getOptional[String]("asura.cluster.hostname")
    val portOpt = configuration.getOptional[Int]("asura.cluster.port")
    val seedNodes = configuration.underlying.getStringList("asura.cluster.seed-nodes")
    val seedNodesStr = if (null != seedNodes && !seedNodes.isEmpty) {
      s""""${seedNodes.asScala.mkString("\",\"")}""""
    } else {
      """"akka://ClusterSystem@127.0.0.1:2551","""
    }
    val roles = configuration.underlying.getStringList("asura.cluster.roles")
    val rolesStr = if (null != roles && !roles.isEmpty) {
      s""""${roles.asScala.mkString("\",\"")}""""
    } else {
      """"indigo""""
    }
    val clusterConfig = ConfigFactory.parseString(
      s"""
         |akka {
         |  remote {
         |    artery {
         |      canonical.hostname = "${hostnameOpt.getOrElse("127.0.0.1")}"
         |      canonical.port = ${portOpt.getOrElse(2551)}
         |    }
         |  }
         |  cluster {
         |    seed-nodes = [${seedNodesStr}]
         |    roles = [${rolesStr}]
         |  }
         |}
       """.stripMargin)
      .withFallback(ConfigFactory.load("cluster"))
    ClusterManager.init(clusterConfig)
  }

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      ClusterManager.shutdown()
      EsClient.closeClient()
    }(system.dispatcher)
  }

  private def toSecurityConfig(configuration: Configuration): SecurityConfig = {
    val pubKeyStrOpt = configuration.getOptional[String]("asura.security.pubKey")
    val priKeyStrOpt = configuration.getOptional[String]("asura.security.priKey")
    val maskText = configuration.getOptional[String]("asura.security.maskText").getOrElse("***")
    if (pubKeyStrOpt.nonEmpty && priKeyStrOpt.nonEmpty) {
      try {
        SecurityConfig(
          pubKeyBytes = Base64.getDecoder.decode(pubKeyStrOpt.get),
          priKeyBytes = Base64.getDecoder.decode(priKeyStrOpt.get),
          maskText = maskText
        )
      } catch {
        case t: Throwable =>
          logger.error(LogUtils.stackTraceToString(t))
          SecurityConfig()
      }
    } else {
      SecurityConfig()
    }
  }

  private def toLinkerdConfig(configuration: Configuration): LinkerdConfig = {
    var enabled = configuration.getOptional[Boolean]("asura.linkerd.enabled").getOrElse(false)
    if (enabled) {
      val serversOpt = configuration.getOptional[ConfigList]("asura.linkerd.servers")
      val servers = if (serversOpt.nonEmpty) {
        val parsedServers = ArrayBuffer[LinkerdConfigServer]()
        serversOpt.get.forEach(config => {
          val value = config.unwrapped().asInstanceOf[java.util.HashMap[String, Any]]
          parsedServers += LinkerdConfigServer(
            tag = value.get("tag").asInstanceOf[String],
            description = value.get("description").asInstanceOf[String],
            namerd = value.get("namerd").asInstanceOf[String],
            proxyHost = value.get("proxyHost").asInstanceOf[String],
            httpProxyPort = value.get("httpProxyPort").asInstanceOf[Int],
            httpsProxyPort = value.get("httpsProxyPort").asInstanceOf[Int],
            headerIdentifier = value.get("headerIdentifier").asInstanceOf[String],
            httpNs = value.get("httpNs").asInstanceOf[String]
          )
        })
        parsedServers
      } else {
        enabled = false
        Nil
      }
      LinkerdConfig(enabled, servers.toSeq)
    } else {
      LinkerdConfig(false, Nil)
    }
  }

  private def toEsOnlineConfigs(listOpt: Option[ConfigList]): Seq[EsOnlineLogConfig] = {
    if (listOpt.nonEmpty) {
      val esConfigs = ArrayBuffer[EsOnlineLogConfig]()
      listOpt.get.forEach(config => {
        val value = config.unwrapped().asInstanceOf[java.util.HashMap[String, Any]]
        esConfigs += EsOnlineLogConfig(
          tag = value.getOrDefault("tag", StringUtils.EMPTY).asInstanceOf[String],
          url = value.get("url").asInstanceOf[String],
          prefix = value.get("prefix").asInstanceOf[String],
          datePattern = value.get("datePattern").asInstanceOf[String],
          fieldDomain = value.get("fieldDomain").asInstanceOf[String],
          fieldMethod = value.get("fieldMethod").asInstanceOf[String],
          fieldUri = value.get("fieldUri").asInstanceOf[String],
          fieldRequestTime = value.get("fieldRequestTime").asInstanceOf[String],
          fieldRequestTimeResolution = value.get("fieldRequestTimeResolution").asInstanceOf[Int].toDouble,
          fieldRemoteAddr = value.get("fieldRemoteAddr").asInstanceOf[String],
          excludeRemoteAddrs = javaListToSeq(value.get("excludeRemoteAddrs").asInstanceOf[java.util.List[String]])
        )
      })
      esConfigs.toSeq
    } else {
      Nil
    }
  }

  private def javaListToSeq(list: java.util.List[String]): Seq[String] = {
    val seq = ArrayBuffer[String]()
    if (null != list) {
      list.forEach(item => seq += item)
    }
    seq.toSeq
  }
}
