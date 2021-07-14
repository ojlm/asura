package asura.ui.cli.args

import asura.ui.cli.runner.ElectronRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(name = "electron")
class ElectronCommand extends ServerBaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ElectronCommand])

  @Option(
    names = Array("--debugger-url"),
    arity = "1",
    paramLabel = "url",
  )
  var debuggerUrl: String = null

  @Option(
    names = Array("--start-url"),
    arity = "1",
    paramLabel = "url",
  )
  var startUrl: String = null

  @Option(
    names = Array("--remote-debugging-port"),
    arity = "1",
    paramLabel = "port",
  )
  var chromePort: Int = 9221

  @Option(
    names = Array("--disable-proxy"),
  )
  var enableProxy: Boolean = true

  @Mixin
  val push: PushMixin = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    ElectronRunner.run(this)
    0
  }

}
