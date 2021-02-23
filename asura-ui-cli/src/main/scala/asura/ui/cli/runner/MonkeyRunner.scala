package asura.ui.cli.runner

import java.util

import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.ChromeRunnerActor
import asura.ui.cli.args.MonkeyCommand
import asura.ui.command.WebMonkeyCommandRunner.MonkeyCommandParams
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.Logger

object MonkeyRunner {

  val logger = Logger("MonkeyRunner")

  def run(args: MonkeyCommand): Unit = {
    if (args.config.exists()) {
      try {
        val config = ConfigFactory.parseFile(args.config)
        val params = JsonUtils.parse(config.root().render(ConfigRenderOptions.concise()), classOf[ConfigParams])
        logger.info(s"config file: \n${JsonUtils.stringifyPretty(params)}")
        if (args.driver.chrome) {
          runChrome(params)
        } else if (args.driver.electron) {
          runElectron(params)
        }
      } catch {
        case t: Throwable =>
          logger.error("{}", t)
      }
    } else {
      logger.error(s"file: ${args.config.getAbsolutePath} not exists.")
    }
  }

  def runChrome(params: ConfigParams): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(params.driver.start))
    options.put("port", Int.box(params.driver.port))
    options.put("headless", Boolean.box(params.driver.headless))
    if (StringUtils.isNotEmpty(params.driver.userDataDir)) {
      options.put("userDataDir", params.driver.userDataDir)
    }
    CliSystem.system.actorOf(ChromeRunnerActor.props(params, options, false, CliSystem.ec))
  }

  def runElectron(params: ConfigParams): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(false))
    options.put("port", Int.box(params.driver.port))
    if (params.driver.debuggerUrl != null) {
      options.put("debuggerUrl", params.driver.debuggerUrl)
    }
    if (params.driver.startUrl != null) {
      options.put("startUrl", params.driver.startUrl)
    }
    CliSystem.system.actorOf(ChromeRunnerActor.props(params, options, true, CliSystem.ec))
  }

  case class ConfigParams(
                           command: CommandParams = CommandParams(),
                           driver: DriverParams = DriverParams(),
                         )

  case class CommandParams(
                            logFile: String = null,
                            params: MonkeyCommandParams = MonkeyCommandParams(),
                          )

  case class DriverParams(
                           logFile: String = null,
                           start: Boolean = true,
                           port: Int = 9222,
                           userDataDir: String = null,
                           headless: Boolean = false,
                           startUrl: String = null,
                           debuggerUrl: String = null,
                         )

}
