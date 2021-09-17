package asura.ui.cli.args

import asura.ui.cli.runner.ChromeRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{Command, Mixin, Option}

@Command(name = "chrome")
class ChromeCommand extends ServerBaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[ChromeCommand])

  @Option(
    names = Array("-s", "--start"),
  )
  var start: Boolean = true

  @Option(
    names = Array("--init-count"),
    arity = "1",
    paramLabel = "num",
  )
  var initCount: Int = 1

  @Option(
    names = Array("--core-count"),
    arity = "1",
    paramLabel = "num",
  )
  var coreCount: Int = 1

  @Option(
    names = Array("--max-count"),
    arity = "1",
    paramLabel = "num",
  )
  var maxCount: Int = 1

  @Option(
    names = Array("--remote-debugging-port"),
    split = ",",
    paramLabel = "port",
  )
  var chromePorts: java.util.List[Integer] = java.util.Arrays.asList(9200)

  @Option(
    names = Array("--user-data-dir"),
    arity = "1",
    paramLabel = "dir",
  )
  var userDataDir: String = null

  @Option(
    names = Array("--not-remove-user-data-dir"),
  )
  var removeUserDataDir: Boolean = true

  @Option(
    names = Array("--user-data-dir-prefix"),
    arity = "1",
    paramLabel = "dir",
  )
  var userDataDirPrefix: String = "target"

  @Option(
    names = Array("--window-position"),
    arity = "1",
    paramLabel = "pos",
  )
  var windowPosition: String = null

  @Option(
    names = Array("--window-size"),
    arity = "1",
    paramLabel = "size",
  )
  var windowSize: String = null

  @Option(
    names = Array("--headless"),
  )
  var headless: Boolean = false

  @Option(
    names = Array("--options"),
    split = ",",
    paramLabel = "option",
  )
  var addOptions: java.util.List[String] = null

  @Option(
    names = Array("--disable-proxy"),
  )
  var enableProxy: Boolean = true

  @Option(
    names = Array("--vnc-pass"),
    arity = "1",
    paramLabel = "***",
  )
  var vncPassword: String = null

  @Option(
    names = Array("--vnc-ws-port"),
    arity = "1",
    paramLabel = "port",
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
