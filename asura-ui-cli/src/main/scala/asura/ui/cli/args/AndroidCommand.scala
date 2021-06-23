package asura.ui.cli.args

import java.io.File

import asura.ui.cli.runner.AndroidRunner
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Android |@"),
  name = "android",
  description = Array("android"),
)
class AndroidCommand extends ServerCommonOptions {

  @Option(
    names = Array("--adb-host"),
    arity = "1",
    paramLabel = "host",
    description = Array("Name of adb server host. Default: localhost.")
  )
  var adbHost: String = "localhost"

  @Option(
    names = Array("--adb-port"),
    arity = "1",
    paramLabel = "port",
    description = Array("Port of adb server. Default:5037.")
  )
  var adbPort: Int = 5037

  @Option(
    names = Array("--adb-path"),
    arity = "1",
    paramLabel = "path",
    description = Array("Full path of the adb program. Use 'adb' in PATH environment default.")
  )
  var adbPath: String = "adb"

  @Option(
    names = Array("--apk"),
    arity = "1",
    paramLabel = "path",
    description = Array("Full path of the apk file. If not set will use the apk in resource bundle.")
  )
  var apk: File = null

  @Option(
    names = Array("-s", "--serial"),
    arity = "1",
    paramLabel = "s",
    description = Array("Use device with given serial.")
  )
  var serial: String = null

  @Option(
    names = Array("--adb-interval"),
    arity = "1",
    paramLabel = "secs",
    description = Array("Interval of check devices. Default: 5 seconds.")
  )
  var checkInterval: Int = 5

  @Option(
    names = Array("--display"),
    description = Array("Display a window, mirror the device screen. Default: true.")
  )
  var display: Boolean = true

  @Option(
    names = Array("--always-on-top"),
    description = Array("Make device window always on top (above other windows).")
  )
  var alwaysOnTop: Boolean = false

  @Option(
    names = Array("--window-width"),
    arity = "1",
    paramLabel = "num",
    description = Array("The initial width of device window. Default: 280")
  )
  var windowWidth: Int = 280

  // below is for the app
  @Option(
    names = Array("--socket-name"),
    arity = "1",
    paramLabel = "name",
    description = Array("Local socket name. Default: asura.")
  )
  var socketName: String = "asura"

  @Option(
    names = Array("--disable-appium-server"),
    description = Array("Disable appium server.")
  )
  var disableAppiumServer: Boolean = false

  @Option(
    names = Array("--enable-appium-http"),
    description = Array("Enable native appium http server.")
  )
  var enableAppiumHttpServer: Boolean = false

  @Option(
    names = Array("--appium-http-port"),
    arity = "1",
    paramLabel = "port",
    description = Array("The port of native appium http server. Default: 6790.")
  )
  var appiumHttpPort: Int = 6790

  @Option(
    names = Array("--enable-appium-mjpeg"),
    description = Array("Enable native appium mjpeg server.")
  )
  var enableAppiumMjpegServer: Boolean = false

  @Option(
    names = Array("--appium-mjpeg-port"),
    arity = "1",
    paramLabel = "port",
    description = Array("The port of native appium mjpeg server. Default: 7810.")
  )
  var appiumMjpegPort: Int = 7810

  @Option(
    names = Array("--disable-scrcpy"),
    description = Array("Disable scrcpy server.")
  )
  var disableScrcpy: Boolean = false

  @Option(
    names = Array("--disable-scrcpy-control"),
    description = Array("Disable scrcpy control server.")
  )
  var disableScrcpyControl: Boolean = false

  @Option(
    names = Array("--bit-rate"),
    arity = "1",
    paramLabel = "num",
    description = Array("Encode the video at the given bit-rate, expressed in bits/s. Default: 8000000.")
  )
  var bitRate: Int = 8000000

  @Option(
    names = Array("--max-fps"),
    arity = "1",
    paramLabel = "num",
    description = Array("Limit the frame rate of screen capture since Android 10.")
  )
  var maxFpx: Int = 0

  @Option(
    names = Array("--display-id"),
    arity = "1",
    paramLabel = "num",
    description = Array("Specify the display id to mirror. Default: 0.")
  )
  var displayId: Int = 0

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    AndroidRunner.run(this)
    0
  }

}
