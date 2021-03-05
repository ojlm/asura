package asura.ui.cli.server

case class ServerProxyConfig(
                              enable: Boolean = false,
                              localChromePort: Int = 0,
                              localWebsockifyPort: Int = 0,
                            ) {

  def isChrome(uri: String): Boolean = {
    enable && localChromePort > 0 && (uri.startsWith("/json") || uri.startsWith("/devtools"))
  }

  def isWebsockify(uri: String): Boolean = {
    enable && localWebsockifyPort > 0 && uri.startsWith("/websockify")
  }

}
