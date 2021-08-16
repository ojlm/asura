package asura.ui.cli.server

import asura.ui.hub.Hubs.{DeviceWdHub, IndigoAppiumHub, IndigoControllerHub}
import asura.ui.hub.Sink
import asura.ui.message.IndigoMessage
import karate.io.netty.channel._

class IndigoMessageHandler(device: String, isController: Boolean) extends SimpleChannelInboundHandler[IndigoMessage] with Sink[IndigoMessage] {

  val wdSinks = DeviceWdHub.getSinks(device)
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
    DeviceWdHub.write(wdSinks, msg)
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


