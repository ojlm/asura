package asura.ui.cli.runner

import java.net.InetSocketAddress
import java.util

import asura.common.util.{HostUtils, NetworkUtils, StringUtils}
import asura.ui.UiConfig
import asura.ui.cli.args.ElectronCommand
import asura.ui.cli.proxy.{TcpProxy, TcpProxyConfig}
import asura.ui.cli.{CliSystem, DriverRegister}
import asura.ui.model.ChromeDriverInfo
import com.typesafe.scalalogging.Logger

object ElectronRunner {

  val logger = Logger("ElectronRunner")

  def run(args: ElectronCommand): Unit = {
    if (args.enableProxy) {
      if (StringUtils.isEmpty(args.proxyIp)) args.proxyIp = NetworkUtils.getLocalIpAddress()
      logger.info(s"proxy: ${args.proxyIp}:${args.proxyPort} => localhost:${args.port}")
      val proxyConfig = TcpProxyConfig(
        new InetSocketAddress(args.proxyIp, args.proxyPort),
        new InetSocketAddress(args.port),
      )
      CliSystem.system.actorOf(TcpProxy.props(proxyConfig), s"tcp-proxy-${args.proxyPort}")
    }
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(false))
    options.put("port", Int.box(args.port))
    if (args.debuggerUrl != null) {
      options.put("debuggerUrl", args.debuggerUrl)
    }
    if (args.startUrl != null) {
      options.put("startUrl", args.startUrl)
    }
    val localChrome = if (args.enablePush) {
      if (args.enableProxy) {
        ChromeDriverInfo(args.proxyIp, args.proxyPort, null, true)
      } else {
        ChromeDriverInfo("localhost", args.port, null, true)
      }
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
      uiDriverProvider = if (args.enablePush) DriverRegister(args.pushUrl) else null,
      syncInterval = args.pushInterval,
      options = options
    )
    UiConfig.init(config)
  }

}
