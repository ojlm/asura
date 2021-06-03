package asura.ui.cli.args

import asura.ui.cli.runner.AndroidRunner
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Android |@"),
  name = "android",
  description = Array("android"),
)
class AndroidCommand extends ServerCommonOptions {

  @Option(
    names = Array("--enable-server"),
    description = Array("Start a local server. Default: true.")
  )
  var enableServer: Boolean = true

  @Option(
    names = Array("--adb-host"),
    arity = "1",
    paramLabel = "HOST",
    description = Array("Name of adb server host. Default: localhost.")
  )
  var adbHost: String = "localhost"

  @Option(
    names = Array("--adb-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Port of adb server. Default:5037.")
  )
  var adbPort: Int = 5037

  @Option(
    names = Array("-s", "--serial"),
    arity = "1",
    paramLabel = "S",
    description = Array("Use device with given serial.")
  )
  var serial: String = null

  @Option(
    names = Array("--adb-interval"),
    arity = "1",
    paramLabel = "SECS",
    description = Array("Interval of check devices. Default: 5 seconds.")
  )
  var checkInterval: Int = 5

  @Option(
    names = Array("--display"),
    description = Array("Display a window, mirror the device screen. Default: true.")
  )
  var display: Boolean = true

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    AndroidRunner.run(this)
    0
  }

}
