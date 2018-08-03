package asura.app.hook

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.core.CoreConfig
import asura.core.es.EsClient
import asura.core.job.JobCenter
import asura.core.job.actor.SchedulerActor
import asura.namerd.NamerdConfig
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration
                                ) {
  val logger = LoggerFactory.getLogger(classOf[ApplicationStart])
  logger.info("application started")
  val config = configuration.underlying
  // init other modules
  if (configuration.getOptional[Boolean]("asura.job.enabled").getOrElse(false)) {
    logger.info("init job modules")
    JobCenter.init(configuration.get[String]("asura.workDir"), configuration.get[String]("asura.reportBaseUrl"))
    import asura.common.config.PropertiesConversions.toProperties
    system.actorOf(SchedulerActor.props(config.getConfig("asura.job.quartz")), "JobScheduler")
  }

  private val materializer = ActorMaterializer()(system)
  CoreConfig.init(CoreConfig(
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer,
    redisServers = Nil,
    useLocalEsNode = configuration.getOptional[Boolean]("asura.es.useLocalNode").getOrElse(false),
    localEsDataDir = configuration.getOptional[String]("asura.es.localEsDataDir").getOrElse("./data"),
    esUrl = configuration.get[String]("asura.es.url"),
    enableLinkerd = configuration.getOptional[Boolean]("asura.linkerd.enableLinkerd").getOrElse(false),
    proxyHost = configuration.getOptional[String]("asura.linkerd.proxyHost").getOrElse(""),
    httpProxyPort = configuration.getOptional[Int]("asura.linkerd.httpProxyPort").getOrElse(4140),
    httpsProxyPort = configuration.getOptional[Int]("asura.linkerd.httpsProxyPort").getOrElse(4143),
    proxyIdentifier = configuration.getOptional[String]("asura.linkerd.headerIdentifier").getOrElse(""),
    reportBaseUrl = configuration.getOptional[String]("asura.reportBaseUrl").getOrElse("")
  ))
  NamerdConfig.init(
    url = configuration.get[String]("asura.linkerd.namerd"),
    system = system,
    dispatcher = system.dispatcher,
    materializer = materializer
  )

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      if (null != EsClient.httpClient) EsClient.httpClient.close()
    }(system.dispatcher)
  }
}
