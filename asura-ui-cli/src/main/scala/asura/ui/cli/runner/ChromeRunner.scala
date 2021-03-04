package asura.ui.cli.runner

import java.util

import asura.common.util.{HostUtils, StringUtils}
import asura.ui.UiConfig
import asura.ui.cli.args.ChromeCommand
import asura.ui.cli.server.{Server, ServerProxyConfig}
import asura.ui.cli.{CliSystem, DriverRegister}
import asura.ui.model.ChromeDriverInfo
import com.typesafe.scalalogging.Logger

object ChromeRunner {

  val logger = Logger("ChromeRunner")

  def run(args: ChromeCommand): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(args.start))
    options.put("port", Int.box(args.chromePort))
    options.put("headless", Boolean.box(args.headless))
    if (StringUtils.isNotEmpty(args.userDataDir)) {
      options.put("userDataDir", args.userDataDir)
    }
    val localChrome = if (args.enableServer && args.push.enablePush) {
      ChromeDriverInfo(
        args.push.pushIp,
        if (args.push.pushPort > 0) args.push.pushPort else args.serverPort,
        args.vncPassword,
      )
    } else {
      null
    }
    if (localChrome != null) localChrome.hostname = HostUtils.hostname
    val config = UiConfig(
      system = CliSystem.system,
      ec = CliSystem.ec,
      taskListener = null,
      enableLocal = true,
      localChrome = localChrome,
      uiDriverProvider = if (args.push.enablePush) DriverRegister(args.push.pushUrl) else null,
      syncInterval = args.push.pushInterval,
      options = options
    )
    UiConfig.init(config)
    if (args.enableServer) {
      val server = Server(args.serverPort, ServerProxyConfig(args.enableProxy, args.chromePort, args.vncWsPort))
      server.start()
      sys.addShutdownHook({
        server.stop()
      })
    }
  }

}
