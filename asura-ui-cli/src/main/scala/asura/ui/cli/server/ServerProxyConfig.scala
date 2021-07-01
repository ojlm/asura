package asura.ui.cli.server

import java.util.concurrent.ConcurrentHashMap

import asura.ui.cli.server.ServerProxyConfig.{DEFAULT_PORT_SELECTOR, PortSelector}

case class ServerProxyConfig(
                              enable: Boolean = false,
                              portSelector: PortSelector = DEFAULT_PORT_SELECTOR,
                              localWebsockifyPort: Int = 0,
                              enableScrcpy: Boolean = true,
                              dumpScrcpy: Boolean = false,
                            ) {

  def isChrome(uri: String): Boolean = {
    enable && (uri.startsWith("/json") || uri.startsWith("/devtools"))
  }

  def isWebsockify(uri: String): Boolean = {
    enable && localWebsockifyPort > 0 && uri.startsWith("/websockify")
  }

}

object ServerProxyConfig {

  val DEFAULT_PORT_SELECTOR = new PortSelector {
    override def getPort(selector: String): Integer = 0
  }

  case class FixedPortSelector(port: Integer) extends PortSelector {
    override def getPort(selector: String): Integer = port
  }

  case class ConcurrentHashMapPortSelector(
                                            map: ConcurrentHashMap[String, Integer] = new ConcurrentHashMap[String, Integer]()
                                          ) extends PortSelector {
    override def getPort(selector: String): Integer = {
      if (selector == null) {
        val iterator = map.entrySet().iterator()
        if (iterator.hasNext) {
          iterator.next().getValue
        } else {
          throw new RuntimeException(s"No port available for $selector")
        }
      } else {
        map.get(selector)
      }
    }

    override def set(selector: String, port: Integer): Unit = map.put(selector, port)

    override def remove(selector: String): Unit = map.remove(selector)
  }

  trait PortSelector {
    def getPort(selector: String): Integer

    def set(selector: String, port: Integer): Unit = {}

    def remove(selector: String): Unit = {}
  }

}
