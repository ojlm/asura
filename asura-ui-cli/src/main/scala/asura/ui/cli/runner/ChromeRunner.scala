package asura.ui.cli.runner

import java.util

import asura.common.util.StringUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.args.ChromeCommand
import asura.ui.cli.push.PushOptions
import asura.ui.cli.server.ServerProxyConfig.ConcurrentHashMapPortSelector
import asura.ui.cli.server.{Server, ServerProxyConfig}
import com.typesafe.scalalogging.Logger

object ChromeRunner {

  val logger = Logger("ChromeRunner")

  def run(args: ChromeCommand): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(args.start))
    options.put("headless", Boolean.box(args.headless))
    options.put("removeUserDataDir", Boolean.box(args.removeUserDataDir))
    if (StringUtils.isNotEmpty(args.windowPosition) || StringUtils.isNotEmpty(args.windowSize)) {
      if (args.addOptions == null) args.addOptions = new util.ArrayList[String]()
      if (StringUtils.isNotEmpty(args.windowPosition)) {
        args.addOptions.add(s"--window-position=${args.windowPosition}")
      }
      if (StringUtils.isNotEmpty(args.windowSize)) {
        args.addOptions.add(s"--window-size=${args.windowSize}")
      }
    }
    options.put("addOptions", args.addOptions)
    if (StringUtils.isNotEmpty(args.userDataDir)) {
      options.put("userDataDir", args.userDataDir)
    }
    val pushOptions = if (args.enableServer && (args.push.enablePushStatus || args.push.enablePushLogs)) {
      PushOptions(
        pushIp = args.push.pushIp,
        pushPort = if (args.push.pushPort > 0) args.push.pushPort else args.serverPort,
        pushUrl = args.push.pushUrl,
        pushInterval = args.push.pushInterval,
        pushStatus = args.push.enablePushStatus,
        pushScreen = args.push.enablePushScreen,
        pushLogs = args.push.enablePushLogs,
        password = args.vncPassword,
      )
    } else {
      null
    }
    val selector = ConcurrentHashMapPortSelector()
    val poolOptions = PoolOptions(
      args.start, args.initCount, args.coreCount, args.maxCount,
      args.userDataDirPrefix, args.removeUserDataDir,
      args.chromePorts, options, pushOptions, selector
    )
    CliSystem.startWebDriverPool(poolOptions)
    if (args.enableServer) {
      val proxyConfig = ServerProxyConfig(
        enable = args.enableProxy,
        portSelector = selector,
        localWebsockifyPort = args.vncWsPort
      )
      val server = Server(args.serverPort, proxyConfig)
      server.start()
      sys.addShutdownHook({
        server.stop()
      })
    }
  }

}
