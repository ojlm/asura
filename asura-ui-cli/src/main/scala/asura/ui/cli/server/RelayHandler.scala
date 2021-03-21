package asura.ui.cli.server

import karate.io.netty.buffer.Unpooled
import karate.io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import karate.io.netty.util.ReferenceCountUtil

class RelayHandler(relayChannel: Channel) extends ChannelInboundHandlerAdapter {

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
      relayChannel.writeAndFlush(msg)
    } else {
      ReferenceCountUtil.release(msg)
    }
  }

}
