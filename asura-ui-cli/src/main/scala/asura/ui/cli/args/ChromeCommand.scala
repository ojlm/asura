package asura.ui.cli.args

import asura.ui.cli.runner.ChromeRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|cyan Start local chrome for remote debugging |@"),
  name = "chrome",
  description = Array("Control the local chrome life cycle"),
)
class ChromeCommand extends ServerBaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ChromeCommand])

  @Option(
    names = Array("-s", "--start"),
    description = Array(
      "Start new chrome instance. Default true. If `false`, ",
      "it will try to attach to the instances on `--remote-debugging-port`."
    )
  )
  var start: Boolean = true

  @Option(
    names = Array("--init-count"),
    arity = "1",
    paramLabel = "num",
    description = Array("The number to start at first. Default 1.")
  )
  var initCount: Int = 1

  @Option(
    names = Array("--core-count"),
    arity = "1",
    paramLabel = "num",
    description = Array("The number of chromes to keep in the pool, even if they are idle. Default 1.")
  )
  var coreCount: Int = 1

  @Option(
    names = Array("--max-count"),
    arity = "1",
    paramLabel = "num",
    description = Array("The maximum number of chromes to allow in the pool. Default 1.")
  )
  var maxCount: Int = 1

  @Option(
    names = Array("--remote-debugging-port"),
    split = ",",
    paramLabel = "port",
    description = Array(
      "Chrome remote debugging ports or any remote ports, default: [9222].",
      "If 'start=true' and only one chrome instance need to start, will use this option.",
      "If 'start=false', will attach to this ports."
    )
  )
  var chromePorts: java.util.List[Integer] = java.util.Arrays.asList(9200)

  @Option(
    names = Array("--user-data-dir"),
    arity = "1",
    paramLabel = "dir",
    description = Array("Chrome user data dir.")
  )
  var userDataDir: String = null

  @Option(
    names = Array("--remove-user-data-dir"),
    description = Array("Remove user data dir after driver quit.")
  )
  var removeUserDataDir: Boolean = true

  @Option(
    names = Array("--user-data-dir-prefix"),
    arity = "1",
    paramLabel = "dir",
    description = Array("Chrome user data prefix dir. Default: 'target'")
  )
  var userDataDirPrefix: String = "target"

  @Option(
    names = Array("--headless"),
    description = Array(
      "Start a headless chrome.",
    )
  )
  var headless: Boolean = false

  @Option(
    names = Array("--options"),
    split = ",",
    paramLabel = "option",
    description = Array(
      "Other chrome options. e.g. '--options ",
      "\"--incognito,--mute-audio,--use-fake-ui-for-media-stream,--use-fake-device-for-media-stream\"'. ",
    )
  )
  var addOptions: java.util.List[String] = null

  @Option(
    names = Array("--enable-proxy"),
    description = Array(
      "Create a local proxy to make the local chrome can be accessed. Default true.",
    )
  )
  var enableProxy: Boolean = true

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
    paramLabel = "port",
    description = Array("Local websockify port. Default: 5901.")
  )
  var vncWsPort: Int = 5901

  @Mixin
  val push: PushMixin = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    if (maxCount < coreCount || coreCount < initCount) {
      logger.error(s"'--max-count($maxCount)'>='--core-count($coreCount)'>='--init-count($initCount)'")
    } else {
      ChromeRunner.run(this)
    }
    0
  }

}
