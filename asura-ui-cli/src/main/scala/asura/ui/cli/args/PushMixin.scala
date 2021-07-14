package asura.ui.cli.args

import asura.common.util.NetworkUtils
import picocli.CommandLine.Option

class PushMixin {

  @Option(
    names = Array("--push-ip"),
    arity = "1",
    paramLabel = "ip",
    descriptionKey = "push.ip",
  )
  var pushIp: String = NetworkUtils.getLocalIpAddress()

  @Option(
    names = Array("--push-port"),
    arity = "1",
    paramLabel = "port",
    descriptionKey = "push.port",
  )
  var pushPort: Int = 0

  @Option(
    names = Array("--push-url"),
    arity = "1",
    paramLabel = "url",
    descriptionKey = "push.url",
  )
  var pushUrl: String = null

  @Option(
    names = Array("--enable-push-status"),
    descriptionKey = "push.enable-push-status",
  )
  var enablePushStatus: Boolean = false

  @Option(
    names = Array("--enable-push-screen"),
    descriptionKey = "push.enable-push-screen",
  )
  var enablePushScreen: Boolean = false

  @Option(
    names = Array("--push-status-interval"),
    arity = "1",
    paramLabel = "secs",
    descriptionKey = "push.push-status-interval",
  )
  var pushInterval: Int = 30

  @Option(
    names = Array("--enable-push-logs"),
    descriptionKey = "push.enable-push-logs",
  )
  var enablePushLogs: Boolean = false

}
