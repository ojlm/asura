package asura.ui.cli.server

import karate.io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import karate.io.netty.handler.codec.http.FullHttpResponse
import karate.io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import karate.io.netty.util.ReferenceCountUtil

class WebSocketRelayHandler(relayChannel: Channel, handshaker: WebSocketClientHandshaker) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    handshaker.handshake(ctx.channel())
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    if (relayChannel.isActive) {
      NettyUtils.flushAndClose(relayChannel)
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (relayChannel.isActive) {
      if (handshaker != null && !handshaker.isHandshakeComplete) {
        handshaker.finishHandshake(ctx.channel(), msg.asInstanceOf[FullHttpResponse])
      } else {
        relayChannel.writeAndFlush(msg)
      }
    } else {
      ReferenceCountUtil.release(msg)
    }
  }

}
