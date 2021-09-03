package asura.ui.cli.push

import java.net.URI

import asura.common.util.StringUtils

case class PushOptions(
                        pushIp: String,
                        pushPort: Int,
                        pushUrl: String,
                        pushInterval: Int,
                        pushStatus: Boolean,
                        pushScreen: Boolean,
                        pushLogs: Boolean,
                        password: String,
                        var electron: Boolean = false,
                      ) {

  def buildClient(): PushEventListener = {
    if (StringUtils.isNotEmpty(pushUrl)) {
      val uri = URI.create(pushUrl)
      val scheme = uri.getScheme
      scheme match {
        case "http" | "https" => HttpPushClient(this)
        case "ws" | "wss" => WebsocketPushClient(this)
        case "tcp" | "unix" => TcpPushClient(this)
        case _ => LogPushEventClient(this)
      }
    } else {
      LogPushEventClient(this)
    }
  }

}
