package asura.ui.cli.runner

import asura.ui.cli.CliSystem
import asura.ui.cli.actor.AndroidRunnerActor
import asura.ui.cli.args.AndroidCommand
import asura.ui.cli.server.{Server, ServerProxyConfig}
import com.typesafe.scalalogging.Logger

object AndroidRunner {

  val logger = Logger("AndroidRunner")

  def run(args: AndroidCommand): Unit = {
    if (args.enableServer) {
      val server = Server(args.serverPort, ServerProxyConfig())
      server.start()
      sys.addShutdownHook({
        server.stop()
      })
    }
    val params = ConfigParams(
      adbHost = args.adbHost,
      adbPort = args.adbPort,
      serial = args.serial,
      checkInterval = args.checkInterval,
      serverPort = if (args.enableServer) args.serverPort else -1,
    )
    CliSystem.system.actorOf(AndroidRunnerActor.props(params, CliSystem.ec), "android")
  }

  case class ConfigParams(
                           adbHost: String,
                           adbPort: Int,
                           serial: String,
                           checkInterval: Int,
                           serverPort: Int,
                         )

}
