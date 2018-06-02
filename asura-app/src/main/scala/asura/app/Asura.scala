package asura

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import asura.app.actor.UserActors
import asura.app.jobs.JobManager
import asura.app.routes.allRoutes
import asura.core.CoreConfig
import asura.core.redis.RedisClient
import asura.namerd.NamerdConfig
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await
import scala.concurrent.duration._

object GlobalImplicits {
  implicit val system = ActorSystem("asura-system")
  implicit val dispatcher = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

object Asura {

  val logger = Logger("Asura")

  def main(args: Array[String]) {
    AppConfig.initLogbackConfig()
    import GlobalImplicits._
    val routes = allRoutes
    val host = AppConfig.appInterface
    val port = AppConfig.appPort
    val bindingFuture = Http().bindAndHandle(routes, host, port)
    logger.info(s"Server online at http://$host:$port/\n")
    initOther()
    sys.addShutdownHook {
      logger.info("Server is termination...")
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
      RedisClient.shutdown()
      Await.result(system.whenTerminated, 30 seconds)
      logger.info("Server is terminated...Bye")
    }
  }

  private def initOther(): Unit = {
    import GlobalImplicits._
    JobManager.init()
    UserActors.init()
    CoreConfig.init(CoreConfig(
      system = system,
      dispatcher = dispatcher,
      materializer = materializer,
      redisServers = AppConfig.redisServer,
      esUrl = AppConfig.esUrl,
      proxyHost = AppConfig.linkerProxyHost,
      httpProxyPort = AppConfig.linkerHttpProxyPort,
      httpsProxyPort = AppConfig.linkerHttpsProxyPort,
      proxyIdentifier = AppConfig.linkerHeaderIdentifier,
      reportBaseUrl = AppConfig.reportBaseUrl
    ))
    NamerdConfig.init(
      url = AppConfig.linkerNamerdUrl,
      system = system,
      dispatcher = dispatcher,
      materializer = materializer
    )
  }
}
