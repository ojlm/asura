package asura.ui.cli.runner

import java.util
import java.util.Collections

import asura.common.util.StringUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.args.ElectronCommand
import asura.ui.cli.push.PushOptions
import asura.ui.cli.server.ServerProxyConfig.FixedPortSelector
import asura.ui.cli.server.{Server, ServerProxyConfig}
import com.typesafe.scalalogging.Logger

object ElectronRunner {

  val logger = Logger("ElectronRunner")

  def run(args: ElectronCommand): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("type", "electron")
    options.put("start", Boolean.box(false))
    options.put("port", Int.box(args.port))
    if (args.debuggerUrl != null) {
      options.put("debuggerUrl", args.debuggerUrl)
    }
    if (args.startUrl != null) {
      options.put("startUrl", args.startUrl)
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
        password = StringUtils.EMPTY,
        electron = true
      )
    } else {
      null
    }
    val selector = FixedPortSelector(args.port)
    val poolOptions = PoolOptions(
      start = false,
      initCount = 1,
      coreCount = 1,
      maxCount = 1,
      userDataDirPrefix = null,
      removeUserDataDir = false,
      ports = Collections.singletonList(args.port),
      driver = options,
      push = pushOptions,
      selector = selector,
    )
    CliSystem.startWebDriverPool(poolOptions)
    if (args.enableServer) {
      val proxyConfig = ServerProxyConfig(
        enable = args.enableProxy,
        portSelector = selector,
      )
      val server = Server(args.serverPort, proxyConfig)
      server.start()
      sys.addShutdownHook({
        server.stop()
      })
    }
  }

}
