package asura.ui.cli.runner

import java.io.File
import java.nio.file.{Files, Paths}
import java.util
import java.util.Collections

import asura.common.util.{NetworkUtils, ResourceUtils}
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.push.PushOptions
import asura.ui.cli.runner.AndroidRunner.{AppOptions, ConfigParams}
import asura.ui.cli.server.ServerProxyConfig.{ConcurrentHashMapPortSelector, FixedPortSelector}
import asura.ui.cli.server.{Server, ServerProxyConfig}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

object IndigoRunner {

  val logger = Logger("IndigoRunner")

  def run(preferred: File): Unit = {
    try {
      val configFile = getConfig(preferred)
      logger.info(s"config.file: ${configFile.getAbsolutePath}")
      val config = ConfigFactory.parseFile(configFile)
      val serverConfig = config.getConfig("server")
      val serverPort = getIntOr("port", serverConfig, 8080)
      val pushOptions = if (config.hasPath("push")) {
        val pushConfig = config.getConfig("push")
        PushOptions(
          pushIp = getStringOr("pushIp", pushConfig, NetworkUtils.getLocalIpAddress()),
          pushPort = getIntOr("pushPort", pushConfig, serverPort),
          pushUrl = getStringOr("pushUrl", pushConfig, null),
          pushInterval = getIntOr("pushInterval", pushConfig, 30),
          pushStatus = getBoolOr("pushStatus", pushConfig, false),
          pushScreen = getBoolOr("pushScreen", pushConfig, false),
          pushLogs = getBoolOr("pushLogs", pushConfig, false),
          password = getStringOr("password", pushConfig, null),
        )
      } else {
        null
      }
      var portSelector = ServerProxyConfig.DEFAULT_PORT_SELECTOR
      if (config.hasPath("web")) {
        val webConfig = config.getConfig("web")
        if (getBoolOr("enabled", webConfig, true)) {
          val isElectron = getBoolOr("electron", webConfig, false)
          if (pushOptions != null) pushOptions.electron = isElectron
          val electronPort = getIntOr("electronPort", webConfig, 9222)
          val options = new util.HashMap[String, Object]()
          if (isElectron) {
            options.put("type", "electron")
            options.put("start", Boolean.box(false))
            options.put("port", Int.box(electronPort))
            if (webConfig.hasPath("startUrl")) {
              options.put("startUrl", getStringOr("startUrl", webConfig, null))
            }
            if (webConfig.hasPath("debuggerUrl")) {
              options.put("debuggerUrl", getStringOr("debuggerUrl", webConfig, null))
            }
          } else {
            options.put("start", Boolean.box(true))
            options.put("headless", Boolean.box(getBoolOr("headless", webConfig, false)))
            options.put("removeUserDataDir", Boolean.box(getBoolOr("removeUserDataDir", webConfig, true)))
            if (webConfig.hasPath("addOptions")) {
              options.put("addOptions", getStringListOr("addOptions", webConfig, Collections.emptyList()))
            }
            if (webConfig.hasPath("userDataDir")) {
              options.put("userDataDir", getStringOr("userDataDir", webConfig, null))
            }
          }
          portSelector = if (isElectron) FixedPortSelector(electronPort) else ConcurrentHashMapPortSelector()
          val webPoolOptions = PoolOptions(
            start = if (isElectron) false else true,
            initCount = if (isElectron) 1 else getIntOr("initCount", webConfig, 0),
            coreCount = if (isElectron) 1 else getIntOr("coreCount", webConfig, 0),
            maxCount = if (isElectron) 1 else getIntOr("maxCount", webConfig, 0),
            userDataDirPrefix = if (isElectron) null else getStringOr("userDataDirPrefix", webConfig, "target"),
            removeUserDataDir = if (isElectron) false else getBoolOr("removeUserDataDir", webConfig, true),
            ports = if (isElectron) Collections.singletonList(electronPort) else Collections.singletonList(9200),
            driver = options,
            push = pushOptions,
            selector = portSelector,
          )
          logger.info("web.pool is started")
          CliSystem.startWebDriverPool(webPoolOptions)
        }
      }
      val server = Server(serverPort, ServerProxyConfig(true, portSelector))
      logger.info(s"server: listen on port $serverPort")
      logger.info(s"open: http://localhost:${serverPort}")
      server.start()
      sys.addShutdownHook({
        logger.info("shutdown")
        server.stop()
      })
      if (config.hasPath("android")) {
        val androidConfig = config.getConfig("android")
        if (getBoolOr("enabled", androidConfig, true)) {
          val params = ConfigParams(
            serverPort = serverPort,
            adbHost = getStringOr("adbHost", androidConfig, "localhost"),
            adbPort = getIntOr("adbPort", androidConfig, 5037),
            adbPath = getStringOr("adbPath", androidConfig, "adb"),
            apk = if (androidConfig.hasPath("apk")) new File(getStringOr("apk", androidConfig, null)) else null,
            serial = getStringOr("serial", androidConfig, null),
            checkInterval = getIntOr("checkInterval", androidConfig, 5),
            display = getBoolOr("display", androidConfig, true),
            windowWidth = getIntOr("windowWidth", androidConfig, 280),
            alwaysOnTop = getBoolOr("alwaysOnTop", androidConfig, false),
            options = AppOptions(
              socketName = getStringOr("socketName", androidConfig, "asura"),
              disableAppiumServer = getBoolOr("disableAppiumServer", androidConfig, false),
              enableAppiumHttpServer = getBoolOr("enableAppiumHttpServer", androidConfig, false),
              appiumHttpPort = getIntOr("appiumHttpPort", androidConfig, 6790),
              enableAppiumMjpegServer = getBoolOr("enableAppiumMjpegServer", androidConfig, false),
              appiumMjpegPort = getIntOr("appiumMjpegPort", androidConfig, 7810),
              disableScrcpy = getBoolOr("disableScrcpy", androidConfig, false),
              disableScrcpyControl = getBoolOr("disableScrcpyControl", androidConfig, false),
              bitRate = getIntOr("bitRate", androidConfig, 8000000),
              maxFps = getIntOr("maxFps", androidConfig, 0),
              displayId = getIntOr("displayId", androidConfig, 0),
            ),
          )
          logger.info("android.pool is started")
          AndroidRunner.run(params, getBoolOr("display", androidConfig, true))
        }
      }
    } catch {
      case t: Throwable => logger.error(t.getMessage)
    }
  }

  def getIntOr(path: String, config: Config, default: Int): Int = {
    if (config.hasPath(path)) config.getInt(path) else default
  }

  def getStringOr(path: String, config: Config, default: String): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }

  def getBoolOr(path: String, config: Config, default: Boolean): Boolean = {
    if (config.hasPath(path)) config.getBoolean(path) else default
  }

  def getStringListOr(path: String, config: Config, default: util.List[String]): util.List[String] = {
    if (config.hasPath(path)) config.getStringList(path) else default
  }

  def getConfig(preferred: File): File = {
    val userHome = System.getProperty("user.home")
    logger.info(s"user.home: $userHome")
    if (preferred != null && preferred.exists() && preferred.isFile) {
      preferred
    } else {
      val indigoDir = Paths.get(userHome, ".indigo").toFile
      val indigoConfig = Paths.get(userHome, ".indigo", "indigo.conf").toFile
      if (indigoDir.exists() && indigoDir.isDirectory) {
        if (indigoConfig.exists() && indigoConfig.isFile) {
          indigoConfig
        } else {
          createAndCopyDefaultConfig(indigoConfig)
        }
      } else {
        if (indigoDir.mkdir()) {
          createAndCopyDefaultConfig(indigoConfig)
        } else {
          throw new RuntimeException(s"Can not create dir: ${indigoDir.getAbsolutePath}")
        }
      }
    }
  }

  def createAndCopyDefaultConfig(target: File): File = {
    val bytes = ResourceUtils.getAsBytes("indigo.conf")
    Files.write(target.toPath, bytes)
    target
  }

  case class IndigoParams()

}
