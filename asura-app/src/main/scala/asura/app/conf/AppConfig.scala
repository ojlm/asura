package asura

import java.util.Properties

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asScalaBuffer

object AppConfig {

  private val config: Config = System.getProperty("asura.env") match {
    case "prod" =>
      ConfigFactory.load("application.prod.conf")
    case _ =>
      ConfigFactory.load("application.test.conf")
  }

  def initLogbackConfig(): Unit = {
    val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    try {
      val configurator = new JoranConfigurator()
      configurator.setContext(context)
      context.reset()
      System.getProperty("asura.env") match {
        case "prod" =>
          configurator.doConfigure(AppConfig.getClass.getClassLoader.getResourceAsStream("logback.prod.xml"))
        case _ =>
          configurator.doConfigure(AppConfig.getClass.getClassLoader.getResourceAsStream("logback.test.xml"))
      }
    } catch {
      case _: Throwable =>
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context)
  }

  // app
  val appInterface = config.getString("app.interface")
  val appPort = config.getInt("app.port")
  val timeoutDefault = config.getInt("timeout.default")
  val appWorkDir = config.getString("app.workDir")
  val reportBaseUrl = config.getString("app.reportBaseUrl")

  // job
  val jobStdLogFileName = config.getString("job.stdLogFileName")
  val jobWorkDir = config.getString("job.workDir")

  // quartz
  import asura.common.config.PropertiesConversions.toProperties

  val quartzParallelConfig: Properties = config.getConfig("quartz.common")
    .withFallback(config.getConfig("quartz.parallel"))
  val quartzSerialConfig: Properties = config.getConfig("quartz.common").
    withFallback(config.getConfig("quartz.serial"))

  // es
  val esUrl = config.getString("es.url")

  // redis
  val redisServer = asScalaBuffer(config.getStringList("redis.servers"))

  // linker
  val linkerNamerdUrl = config.getString("linker.namerd")
  val linkerHttpNs = config.getString("linker.httpNs")
  val linkerSocksNs = config.getString("linker.socksNs")
  val linkerTcpNs = config.getString("linker.tcpNs")
  val linkerProxyHost = config.getString("linker.proxyHost")
  val linkerHttpProxyPort = config.getInt("linker.httpProxyPort")
  val linkerHttpsProxyPort = config.getInt("linker.httpsProxyPort")
  val linkerHeaderIdentifier = config.getString("linker.headerIdentifier")
}
