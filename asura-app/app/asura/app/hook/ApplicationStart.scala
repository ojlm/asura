package asura.app.hook

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.app.api.auth.BasicAuth
import asura.app.notify.MailNotifier
import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.CoreConfig.EsOnlineLogConfig
import asura.core.auth.AuthManager
import asura.core.es.EsClient
import asura.core.job.JobCenter
import asura.core.job.actor.SchedulerActor
import asura.core.notify.JobNotifyManager
import asura.namerd.NamerdConfig
import com.typesafe.config.ConfigList
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
    enableProxy = configuration.getOptional[Boolean]("asura.linkerd.enabled").getOrElse(false),
    proxyHost = configuration.getOptional[String]("asura.linkerd.proxyHost").getOrElse(""),
    httpProxyPort = configuration.getOptional[Int]("asura.linkerd.httpProxyPort").getOrElse(4140),
    httpsProxyPort = configuration.getOptional[Int]("asura.linkerd.httpsProxyPort").getOrElse(4143),
    proxyIdentifier = configuration.getOptional[String]("asura.linkerd.headerIdentifier").getOrElse(""),
    reportBaseUrl = configuration.getOptional[String]("asura.reportBaseUrl").getOrElse(""),
    onlineConfigs = toEsOnlineConfigs(configuration.getOptional[ConfigList]("asura.es.onlineLog"))
  ))
  NamerdConfig.init(
    url = configuration.get[String]("asura.linkerd.namerd"),
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer
  )

  // add auth
  AuthManager.register(BasicAuth)

  // add notify
  JobNotifyManager.register(MailNotifier(mailerClient))

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      EsClient.closeClient()
    }(system.dispatcher)
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
        )
      })
      esConfigs
    } else {
      Nil
    }
  }
}
