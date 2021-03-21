package asura.ui.cli.server

import karate.io.netty.channel.ChannelInitializer
import karate.io.netty.channel.socket.SocketChannel
import karate.io.netty.handler.ssl.SslContext

class ServerInitializer(
                         sslCtx: SslContext,
                         proxyConfig: ServerProxyConfig,
                       ) extends ChannelInitializer[SocketChannel] {

  override def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline()
      .addLast(new PortUnificationServerHandler(sslCtx, proxyConfig))
  }

}
