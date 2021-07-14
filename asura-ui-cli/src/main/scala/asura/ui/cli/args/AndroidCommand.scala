package asura.ui.cli.args

import java.io.File

import asura.ui.cli.runner.AndroidRunner
import picocli.CommandLine.{Command, Mixin, Option}

@Command(name = "android")
class AndroidCommand extends ServerCommonOptions {

  @Option(
    names = Array("--adb-host"),
    arity = "1",
    paramLabel = "host",
  )
  var adbHost: String = "localhost"

  @Option(
    names = Array("--adb-port"),
    arity = "1",
    paramLabel = "port",
  )
  var adbPort: Int = 5037

  @Option(
    names = Array("--adb-path"),
    arity = "1",
    paramLabel = "path",
  )
  var adbPath: String = "adb"

  @Option(
    names = Array("--apk"),
    arity = "1",
    paramLabel = "path",
  )
  var apk: File = null

  @Option(
    names = Array("-s", "--serial"),
    arity = "1",
    paramLabel = "s",
  )
  var serial: String = null

  @Option(
    names = Array("--adb-interval"),
    arity = "1",
    paramLabel = "secs",
  )
  var checkInterval: Int = 5

  @Option(
    names = Array("--disable-display"),
  )
  var display: Boolean = true

  @Option(
    names = Array("--always-on-top"),
  )
  var alwaysOnTop: Boolean = false

  @Option(
    names = Array("--window-width"),
    arity = "1",
    paramLabel = "num",
  )
  var windowWidth: Int = 280

  // below is for the app
  @Option(
    names = Array("--socket-name"),
    arity = "1",
    paramLabel = "name",
  )
  var socketName: String = "asura"

  @Option(
    names = Array("--disable-appium-server"),
  )
  var disableAppiumServer: Boolean = false

  @Option(
    names = Array("--enable-appium-http"),
  )
  var enableAppiumHttpServer: Boolean = false

  @Option(
    names = Array("--appium-http-port"),
    arity = "1",
    paramLabel = "port",
  )
  var appiumHttpPort: Int = 6790

  @Option(
    names = Array("--enable-appium-mjpeg"),
  )
  var enableAppiumMjpegServer: Boolean = false

  @Option(
    names = Array("--appium-mjpeg-port"),
    arity = "1",
    paramLabel = "port",
  )
  var appiumMjpegPort: Int = 7810

  @Option(
    names = Array("--disable-scrcpy"),
  )
  var disableScrcpy: Boolean = false

  @Option(
    names = Array("--disable-scrcpy-control"),
  )
  var disableScrcpyControl: Boolean = false

  @Option(
    names = Array("--bit-rate"),
    arity = "1",
    paramLabel = "num",
  )
  var bitRate: Int = 8000000

  @Option(
    names = Array("--max-fps"),
    arity = "1",
    paramLabel = "num",
  )
  var maxFpx: Int = 0

  @Option(
    names = Array("--display-id"),
    arity = "1",
    paramLabel = "num",
  )
  var displayId: Int = 0

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    AndroidRunner.run(this)
    0
  }

}
