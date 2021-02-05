package asura.ui.cli.proxy

import java.net.InetSocketAddress

case class TcpProxyConfig(
                           local: InetSocketAddress,
                           remote: InetSocketAddress,
                         )
