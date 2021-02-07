package asura.ui.cli.proxy

import java.net.InetSocketAddress

import akka.actor.ActorSystem

object TcpProxySpec {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("tcp-proxy")
    val config = TcpProxyConfig(
      new InetSocketAddress(4201),
      new InetSocketAddress(4200),
    )
    system.actorOf(TcpProxy.props(config))
  }

}
