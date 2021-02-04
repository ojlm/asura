package asura.ui.cli.args

import asura.ui.cli.runner.ChromeRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Option}

@Command(
  header = Array("@|cyan Start a local chrome for remote debugging |@"),
  name = "chrome",
  description = Array("Control the local chrome life cycle"),
)
class ChromeCommand extends BaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ChromeCommand])

  @Option(
    names = Array("-s", "--start"),
    description = Array(
      "Start a new chrome. Default true. If `false`, ",
      "it will try to attach to the instance in PORT."
    )
  )
  var start: Boolean = true

  @Option(
    names = Array("-p", "--remote-debugging-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Chrome remote debugging port, default: 9222.")
  )
  var port: Int = 9222

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
    names = Array("--proxy-ip"),
    arity = "1",
    paramLabel = "IP",
    description = Array(
      "Local proxy ip. If not set and proxy is enabled, will select one automatically",
    )
  )
  var proxyIp: String = null

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

  override def call(): Int = {
    ChromeRunner.run(this)
    0
  }

}
