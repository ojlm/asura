package asura.ui.cli.args

import asura.common.util.NetworkUtils
import asura.ui.cli.runner.ElectronRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Electron |@"),
  name = "electron",
  description = Array("Debug a local electron app"),
)
class ElectronCommand extends BaseCommand {

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
    names = Array("-p", "--remote-debugging-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Chrome remote debugging port or any remote port, default: 9221.")
  )
  var port: Int = 9221

  @Option(
    names = Array("--enable-proxy"),
    description = Array(
      "Create a local proxy to make the local chrome can be accessed. Default false.",
    )
  )
  var enableProxy: Boolean = false

  @Option(
    names = Array("--proxy-ip"),
    arity = "1",
    paramLabel = "IP",
    description = Array(
      "Local proxy ip. If not set and proxy is enabled, will select one automatically",
    )
  )
  var proxyIp: String = NetworkUtils.getLocalIpAddress()

  @Option(
    names = Array("--proxy-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Local proxy port, default: 9223.")
  )
  var proxyPort: Int = 9223

  @Option(
    names = Array("--enable-push"),
    description = Array(
      "Push current driver info to remote server. Default false.",
    )
  )
  var enablePush: Boolean = false

  @Option(
    names = Array("--push-url"),
    arity = "1",
    paramLabel = "URL",
    description = Array("Remote sync post url, Default: http://localhost:9000")
  )
  var pushUrl: String = "http://localhost:9000"

  @Option(
    names = Array("--push-interval"),
    arity = "1",
    paramLabel = "SECS",
    description = Array("Interval of push event, default: 30 seconds.")
  )
  var pushInterval: Int = 30

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    ElectronRunner.run(this)
    0
  }

}
