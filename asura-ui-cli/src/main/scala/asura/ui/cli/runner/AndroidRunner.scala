package asura.ui.cli.runner

import java.io.File

import asura.ui.cli.CliSystem
import asura.ui.cli.args.AndroidCommand
import asura.ui.cli.runner.AndroidRunner.logger
import asura.ui.cli.server.{Server, ServerProxyConfig}
import com.typesafe.scalalogging.Logger
import javafx.application.{Application, Platform}
import javafx.stage.Stage

class AndroidRunner extends Application {

  override def start(primaryStage: Stage): Unit = {
    Platform.setImplicitExit(false)
    primaryStage.setOnCloseRequest(event => {
      event.consume()
    })
  }

  override def stop(): Unit = {
    logger.info("window is closed.")
  }

}

object AndroidRunner {

  val logger = Logger("AndroidRunner")

  def run(args: AndroidCommand): Unit = {
    val server = Server(args.serverPort, ServerProxyConfig())
    server.start()
    sys.addShutdownHook({
      server.stop()
    })
    val params = ConfigParams(
      serverPort = args.serverPort,
      adbHost = args.adbHost,
      adbPort = args.adbPort,
      adbPath = args.adbPath,
      apk = args.apk,
      serial = args.serial,
      checkInterval = args.checkInterval,
      display = args.display,
      windowWidth = args.windowWidth,
      alwaysOnTop = args.alwaysOnTop,
      options = AppOptions(args),
    )
    run(params, args.display)
  }

  def run(params: ConfigParams, display: Boolean): Unit = {
    CliSystem.startAndroidRunner(params)
    if (display) {
      Application.launch(classOf[AndroidRunner], new Array[String](0): _*)
    }
  }

  case class ConfigParams(
                           serverPort: Int,
                           adbHost: String,
                           adbPort: Int,
                           adbPath: String,
                           apk: File,
                           serial: String,
                           checkInterval: Int,
                           display: Boolean,
                           alwaysOnTop: Boolean,
                           windowWidth: Int,
                           options: AppOptions,
                         )

  case class AppOptions(
                         var socketName: String,
                         var disableAppiumServer: Boolean = false,
                         var enableAppiumHttpServer: Boolean = false,
                         var appiumHttpPort: Int = 6790,
                         var enableAppiumMjpegServer: Boolean = false,
                         var appiumMjpegPort: Int = 7810,
                         var disableScrcpy: Boolean = false,
                         var disableScrcpyControl: Boolean = false,
                         var bitRate: Int = 8000000,
                         var maxFps: Int = 0,
                         var displayId: Int = 0,
                       ) {

    def toOptions(): String = {
      val sb = new StringBuilder()
      sb.append("--socket-name ").append(socketName).append(" ")
      if (disableAppiumServer) {
        sb.append("--disable-appium-server ")
      } else {
        if (enableAppiumHttpServer) {
          sb.append("--enable-appium-http ")
          sb.append("--appium-http-port ").append(appiumHttpPort).append(" ")
        }
        if (enableAppiumMjpegServer) {
          sb.append("--enable-appium-mjpeg ")
          sb.append("--appium-mjpeg-port ").append(appiumMjpegPort).append(" ")
        }
      }
      if (disableScrcpy) {
        sb.append("--disable-scrcpy ")
      } else {
        if (disableScrcpyControl) {
          sb.append("--disable-scrcpy-control ")
        }
        sb.append("--bit-rate ").append(bitRate).append(" ")
        if (maxFps > 0) {
          sb.append("--max-fps ").append(maxFps).append(" ")
        }
        sb.append("--display ").append(displayId)
      }
      sb.toString()
    }

  }

  object AppOptions {
    def apply(args: AndroidCommand): AppOptions = {
      val options = AppOptions(args.socketName)
      options.disableAppiumServer = args.disableAppiumServer
      options.enableAppiumHttpServer = args.enableAppiumHttpServer
      options.appiumHttpPort = args.appiumHttpPort
      options.enableAppiumMjpegServer = args.enableAppiumMjpegServer
      options.appiumMjpegPort = args.appiumMjpegPort
      options.disableScrcpy = args.disableScrcpy
      options.disableScrcpyControl = args.disableScrcpyControl
      options.bitRate = args.bitRate
      options.maxFps = args.maxFpx
      options.displayId = args.displayId
      options
    }
  }

}
