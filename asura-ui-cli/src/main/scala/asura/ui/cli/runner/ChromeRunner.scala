package asura.ui.cli.runner

import java.util

import asura.common.util.StringUtils
import asura.ui.UiConfig
import asura.ui.cli.args.ChromeCommand
import asura.ui.cli.{CliSystem, DriverRegister}

object ChromeRunner {

  def run(args: ChromeCommand): Unit = {
    if (args.enableProxy) {
      // TODO: create a proxy actor
    }
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(args.start))
    options.put("port", Int.box(args.port))
    options.put("headless", Boolean.box(args.headless))
    if (StringUtils.isNotEmpty(args.userDataDir)) {
      options.put("userDataDir", args.userDataDir)
    }
    val config = UiConfig(
      system = CliSystem.system,
      ec = CliSystem.ec,
      taskListener = null,
      localChrome = null,
      uiDriverProvider = if (args.enablePush) DriverRegister(args.pushUrl) else null,
      syncInterval = args.pushInterval,
      options = options
    )
    UiConfig.init(config)
  }

}
