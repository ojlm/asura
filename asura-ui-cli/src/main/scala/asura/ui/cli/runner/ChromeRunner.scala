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
        ChromeDriverInfo(
          if (StringUtils.isNotEmpty(args.pushIp)) args.pushIp else args.proxyIp,
          if (args.pushPort > 0) args.pushPort else args.proxyPort,
          null,
        )
      } else {
        ChromeDriverInfo(
          if (StringUtils.isNotEmpty(args.pushIp)) args.pushIp else args.host,
          if (args.pushPort > 0) args.pushPort else args.port,
          null,
        )
      }
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
      uiDriverProvider = if (args.enablePush) DriverRegister(args.pushUrl) else null,
      syncInterval = args.pushInterval,
      options = options
    )
    UiConfig.init(config)
  }

}
