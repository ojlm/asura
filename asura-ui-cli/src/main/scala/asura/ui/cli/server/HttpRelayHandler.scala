package asura.ui.cli.server

import karate.io.netty.buffer.Unpooled
import karate.io.netty.channel.{Channel, ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import karate.io.netty.handler.codec.http.{FullHttpResponse, HttpHeaderNames, HttpHeaderValues}
import karate.io.netty.util.ReferenceCountUtil

class HttpRelayHandler(relayChannel: Channel, enableKeepAlive: Boolean) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    if (relayChannel.isActive) {
      NettyUtils.flushAndClose(relayChannel)
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (relayChannel.isActive) {
      if (msg.isInstanceOf[FullHttpResponse] && !enableKeepAlive) {
        msg.asInstanceOf[FullHttpResponse].headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        val channelFuture = relayChannel.writeAndFlush(msg)
        channelFuture.addListener(ChannelFutureListener.CLOSE)
      } else {
        relayChannel.writeAndFlush(msg)
      }
    } else {
      ReferenceCountUtil.release(msg)
    }
  }

}
