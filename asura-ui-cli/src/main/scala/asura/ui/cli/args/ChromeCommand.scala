package asura.ui.cli.args

import asura.ui.cli.runner.ChromeRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Start a local chrome for remote debugging |@"),
  name = "chrome",
  description = Array("Control the local chrome life cycle"),
)
class ChromeCommand extends ServerBaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ChromeCommand])

  @Option(
    names = Array("-s", "--start"),
    description = Array(
      "Start a new chrome. Default true. If `false`, ",
      "it will try to attach to the instance on `--remote-debugging-port`."
    )
  )
  var start: Boolean = true

  @Option(
    names = Array("--remote-debugging-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Chrome remote debugging port or any remote port, default: 9222.")
  )
  var chromePort: Int = 9222

  @Option(
    names = Array("--user-data-dir"),
    arity = "1",
    paramLabel = "DIR",
    description = Array("Chrome user data dir.")
  )
  var userDataDir: String = null

  @Option(
    names = Array("--headless"),
    description = Array(
      "Start a headless chrome.",
    )
  )
  var headless: Boolean = false

  @Option(
    names = Array("--enable-proxy"),
    description = Array(
      "Create a local proxy to make the local chrome can be accessed. Default false.",
    )
  )
  var enableProxy: Boolean = false

  @Option(
    names = Array("--vnc-pass"),
    arity = "1",
    paramLabel = "***",
    description = Array("VNC password.")
  )
  var vncPassword: String = null

  @Option(
    names = Array("--vnc-ws-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Local websockify port. Default: 5901.")
  )
  var vncWsPort: Int = 5901

  @Mixin
  val push: PushMixin = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    ChromeRunner.run(this)
    0
  }

}
