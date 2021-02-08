package asura.ui.cli.runner

import java.net.InetSocketAddress
import java.util

import asura.common.util.{HostUtils, NetworkUtils, StringUtils}
import asura.ui.UiConfig
import asura.ui.cli.args.ChromeCommand
import asura.ui.cli.proxy.{TcpProxy, TcpProxyConfig}
import asura.ui.cli.{CliSystem, DriverRegister}
import asura.ui.model.ChromeDriverInfo
import com.typesafe.scalalogging.Logger

object ChromeRunner {

  val logger = Logger("ChromeRunner")

  def run(args: ChromeCommand): Unit = {
    if (args.enableProxy) {
      if (StringUtils.isEmpty(args.proxyIp)) args.proxyIp = NetworkUtils.getLocalIpAddress()
      logger.info(s"proxy: ${args.proxyIp}:${args.proxyPort} => localhost:${args.port}")
      val proxyConfig = TcpProxyConfig(
        new InetSocketAddress(args.proxyIp, args.proxyPort),
        new InetSocketAddress(args.host, args.port),
      )
      CliSystem.system.actorOf(TcpProxy.props(proxyConfig), s"tcp-proxy-${args.proxyPort}")
    }
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(args.start))
    options.put("port", Int.box(args.port))
    options.put("headless", Boolean.box(args.headless))
    if (StringUtils.isNotEmpty(args.userDataDir)) {
      options.put("userDataDir", args.userDataDir)
    }
    val localChrome = if (args.enablePush) {
      if (args.enableProxy) {
        ChromeDriverInfo(args.proxyIp, args.proxyPort, null)
      } else {
        ChromeDriverInfo(args.host, args.port, null)
      }
    } else {
      null
    }
    if (localChrome != null) localChrome.hostname = HostUtils.hostname
    val config = UiConfig(
      system = CliSystem.system,
      ec = CliSystem.ec,
      taskListener = null,
      localChrome = localChrome,
      uiDriverProvider = if (args.enablePush) DriverRegister(args.pushUrl) else null,
      syncInterval = args.pushInterval,
      options = options
    )
    UiConfig.init(config)
  }

}
