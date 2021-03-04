package asura.ui.cli.runner

import java.util

import asura.common.util.HostUtils
import asura.ui.UiConfig
import asura.ui.cli.args.ElectronCommand
import asura.ui.cli.server.{Server, ServerProxyConfig}
import asura.ui.cli.{CliSystem, DriverRegister}
import asura.ui.model.ChromeDriverInfo
import com.typesafe.scalalogging.Logger

object ElectronRunner {

  val logger = Logger("ElectronRunner")

  def run(args: ElectronCommand): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(false))
    options.put("port", Int.box(args.chromePort))
    if (args.debuggerUrl != null) {
      options.put("debuggerUrl", args.debuggerUrl)
    }
    if (args.startUrl != null) {
      options.put("startUrl", args.startUrl)
    }
    val localChrome = if (args.enableServer && args.push.enablePush) {
      ChromeDriverInfo(
        args.push.pushIp,
        if (args.push.pushPort > 0) args.push.pushPort else args.serverPort,
        null, true
      )
    } else {
      null
    }
    if (localChrome != null) {
      localChrome.hostname = HostUtils.hostname
      localChrome.startUrl = args.startUrl
      localChrome.debuggerUrl = args.debuggerUrl
    }
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
      val server = Server(args.serverPort, ServerProxyConfig(args.enableProxy, args.chromePort))
      server.start()
      sys.addShutdownHook({
        server.stop()
      })
    }
  }

}
