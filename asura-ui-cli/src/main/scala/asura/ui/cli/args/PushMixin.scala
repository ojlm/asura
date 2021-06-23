package asura.ui.cli.args

import asura.common.util.NetworkUtils
import picocli.CommandLine.Option

class PushMixin {

  @Option(
    names = Array("--enable-push"),
    description = Array(
      "Push current driver info to remote server. Default false.",
    )
  )
  var enablePush: Boolean = false

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
    description = Array("Remote sync post url, Default: http://localhost:9000")
  )
  var pushUrl: String = "http://localhost:9000"

  @Option(
    names = Array("--push-interval"),
    arity = "1",
    paramLabel = "secs",
    description = Array("Interval of push event, default: 30 seconds.")
  )
  var pushInterval: Int = 30

}
