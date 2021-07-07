package asura.ui.cli.args

import asura.common.util.NetworkUtils
import picocli.CommandLine.Option

class PushMixin {

  @Option(
    names = Array("--push-ip"),
    arity = "1",
    paramLabel = "ip",
    description = Array("Used to push to server.")
  )
  var pushIp: String = NetworkUtils.getLocalIpAddress()

  @Option(
    names = Array("--push-port"),
    arity = "1",
    paramLabel = "port",
    description = Array("Used to push to server, default the same with server port.")
  )
  var pushPort: Int = 0

  @Option(
    names = Array("--push-url"),
    arity = "1",
    paramLabel = "url",
    description = Array("Remote push url. e.g. 'http','ws','tcp','unix'")
  )
  var pushUrl: String = null

  @Option(
    names = Array("--enable-push-status"),
    description = Array(
      "Push current driver info to remote server. Default false.",
    )
  )
  var enablePushStatus: Boolean = false

  @Option(
    names = Array("--enable-push-screen"),
    description = Array(
      "Push screenshot to remote server. Default false.",
    )
  )
  var enablePushScreen: Boolean = false

  @Option(
    names = Array("--push-status-interval"),
    arity = "1",
    paramLabel = "secs",
    description = Array("Interval of push chrome status event, default: 30 seconds.")
  )
  var pushInterval: Int = 30

  @Option(
    names = Array("--enable-push-logs"),
    description = Array(
      "Push logs captured from devtools. Default false.",
    )
  )
  var enablePushLogs: Boolean = false

}
