package asura.app.hook

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.app.api.auth.BasicAuth
import asura.app.notify.MailNotifier
import asura.cluster.ClusterManager
import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.CoreConfig.{EsOnlineLogConfig, LinkerdConfig, LinkerdConfigServer}
import asura.core.auth.AuthManager
import asura.core.es.EsClient
import asura.core.job.JobCenter
import asura.core.job.actor.SchedulerActor
import asura.core.notify.JobNotifyManager
import asura.namerd.NamerdConfig
import com.typesafe.config.{ConfigFactory, ConfigList}
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.mailer.MailerClient

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration,
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
  private val materializer = ActorMaterializer()(system)
  CoreConfig.init(CoreConfig(
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer,
    redisServers = Nil,
    useLocalEsNode = configuration.getOptional[Boolean]("asura.es.useLocalNode").getOrElse(false),
    localEsDataDir = configuration.getOptional[String]("asura.es.localEsDataDir").getOrElse("./data"),
    esIndexPrefix = configuration.getOptional[String]("asura.es.indexPrefix"),
    esUrl = configuration.get[String]("asura.es.url"),
    linkerdConfig = toLinkerdConfig(configuration),
    reportBaseUrl = configuration.getOptional[String]("asura.reportBaseUrl").getOrElse(""),
    onlineConfigs = toEsOnlineConfigs(configuration.getOptional[ConfigList]("asura.es.onlineLog"))
  ))
  NamerdConfig.init(
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer
  )

  // add auth
  AuthManager.register(BasicAuth)

  // add notify
  JobNotifyManager.register(MailNotifier(mailerClient))

  if (configuration.getOptional[Boolean]("asura.cluster.enabled").getOrElse(false)) {
    val hostnameOpt = configuration.getOptional[String]("asura.cluster.hostname")
    val portOpt = configuration.getOptional[Int]("asura.cluster.port")
    val seedNodes = configuration.underlying.getStringList("asura.cluster.seed-nodes")
    import scala.collection.JavaConverters.asScalaBuffer
    val seedNodesStr = if (null != seedNodes && !seedNodes.isEmpty) {
      s""""${asScalaBuffer(seedNodes).mkString("\",\"")}""""
    } else {
      """"akka://ClusterSystem@127.0.0.1:2551","""
    }
    val roles = configuration.underlying.getStringList("asura.cluster.roles")
    val rolesStr = if (null != roles && !roles.isEmpty) {
      s""""${asScalaBuffer(roles).mkString("\",\"")}""""
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
      LinkerdConfig(enabled, servers)
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
      esConfigs
    } else {
      Nil
    }
  }

  private def javaListToSeq(list: java.util.List[String]): Seq[String] = {
    val seq = ArrayBuffer[String]()
    if (null != list) {
      list.forEach(item => seq += item)
    }
    seq
  }
}
