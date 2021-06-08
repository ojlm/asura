package asura.ui.cli.server

import asura.ui.cli.hub.Hubs.{IndigoAppiumHub, IndigoControllerHub}
import asura.ui.cli.hub.Sink
import asura.ui.cli.message.IndigoMessage
import karate.io.netty.channel._

class IndigoMessageHandler(device: String, isController: Boolean) extends SimpleChannelInboundHandler[IndigoMessage] with Sink[IndigoMessage] {

  var channel: Channel = null

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    channel = ctx.channel()
    if (isController) {
      IndigoControllerHub.enter(device, this)
    } else {
      IndigoAppiumHub.enter(device, this)
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    super.channelInactive(ctx)
    if (isController) {
      IndigoControllerHub.leave(device, this)
    } else {
      IndigoAppiumHub.leave(device, this)
    }
  }

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, msg: IndigoMessage): Unit = {
    //  there is no input msg now
  }

  override def write(frame: IndigoMessage): Boolean = {
    if (channel.isActive && channel.isWritable) {
      channel.writeAndFlush(frame)
      true
    } else {
      false
    }
  }

  override def close(): Unit = {
    if (channel.isActive) {
      channel.close()
    }
  }

}


