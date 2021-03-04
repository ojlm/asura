package asura.ui.cli.server

import io.netty.karate.channel.ChannelInitializer
import io.netty.karate.channel.socket.SocketChannel
import io.netty.karate.handler.ssl.SslContext

class ServerInitializer(
                         sslCtx: SslContext,
                         proxyConfig: ServerProxyConfig,
                       ) extends ChannelInitializer[SocketChannel] {

  override def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline()
      .addLast(new PortUnificationServerHandler(sslCtx, proxyConfig))
  }

}
