package asura.ui.cli.args

import asura.ui.cli.runner.ElectronRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Electron |@"),
  name = "electron",
  description = Array("Debug a local electron app"),
)
class ElectronCommand extends SubBaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ElectronCommand])

  @Option(
    names = Array("--debugger-url"),
    arity = "1",
    paramLabel = "URL",
    description = Array("Websocket debug url.")
  )
  var debuggerUrl: String = null

  @Option(
    names = Array("--start-url"),
    arity = "1",
    paramLabel = "URL",
    description = Array("Start url for debugging.")
  )
  var startUrl: String = null

  @Option(
    names = Array("--remote-debugging-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Chrome remote debugging port or any remote port, default: 9221.")
  )
  var chromePort: Int = 9221

  @Option(
    names = Array("--enable-proxy"),
    description = Array(
      "Create a local proxy to make the local chrome can be accessed. Default false.",
    )
  )
  var enableProxy: Boolean = false

  @Mixin
  val push: PushMixin = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    ElectronRunner.run(this)
    0
  }

}
