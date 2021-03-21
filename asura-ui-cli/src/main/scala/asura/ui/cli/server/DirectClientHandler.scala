package asura.ui.cli.server

import karate.io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import karate.io.netty.util.concurrent.Promise

class DirectClientHandler(promise: Promise[Channel]) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.pipeline().remove(this)
    promise.setSuccess(ctx.channel())
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    promise.setFailure(cause)
  }

}
