package asura.app.hook

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.app.api.auth.BasicAuth
import asura.app.notify.MailNotifier
import asura.core.CoreConfig
import asura.core.auth.AuthManager
import asura.core.es.EsClient
import asura.core.job.JobCenter
import asura.core.job.actor.SchedulerActor
import asura.core.notify.JobNotifyManager
import asura.namerd.NamerdConfig
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.mailer.MailerClient

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
    onlineLogUrl = configuration.getOptional[String]("asura.es.onlineLogUrl").getOrElse(""),
    onlineLogIndexPrefix = configuration.getOptional[String]("asura.es.onlineLogPrefix").getOrElse(""),
    onlineLogDatePattern = configuration.getOptional[String]("asura.es.onlineLogDatePattern").getOrElse(""),
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
      if (null != EsClient.esClient) EsClient.esClient.close()
    }(system.dispatcher)
  }
}
