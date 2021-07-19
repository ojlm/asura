package asura.ui.cli.server

import java.net.URI

import karate.io.netty.channel.{Channel, ChannelHandler}
import karate.io.netty.handler.codec.http.DefaultHttpHeaders
import karate.io.netty.handler.codec.http.websocketx.{WebSocketClientHandshakerFactory, WebSocketDecoderConfig, WebSocketServerProtocolConfig, WebSocketVersion}

case class WebSocketProxyConfig(
                                 uri: URI,
                                 serverConfig: WebSocketServerProtocolConfig,
                               ) {
  def clientHandler(relayChannel: Channel): ChannelHandler = {
    new WebSocketRelayHandler(relayChannel, WebSocketClientHandshakerFactory.newHandshaker(
      uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(),
      104857600, true, false
    ))
  }

  def serverHandler(relayChannel: Channel): ChannelHandler = {
    new WebSocketRelayHandler(relayChannel, null)
  }
}

object WebSocketProxyConfig {

  def devtoolsPage(uri: URI): WebSocketProxyConfig = {
    WebSocketProxyConfig(
      uri = uri,
      serverConfig = WebSocketServerProtocolConfig.newBuilder()
        .websocketPath("/devtools/page")
        .subprotocols(null)
        .checkStartsWith(true)
        .handshakeTimeoutMillis(10000L)
        .dropPongFrames(true)
        .handleCloseFrames(true)
        .decoderConfig(
          WebSocketDecoderConfig.newBuilder()
            .maxFramePayloadLength(104857600) // 100M
            .allowMaskMismatch(false)
            .allowExtensions(true)
            .build()
        ).build()
    )
  }

}
