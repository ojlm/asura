package asura.ui.cli.server

import asura.ui.cli.hub.Hubs.{ControllerHub, ReceiverHub}
import asura.ui.cli.hub.{ControlMessage, DeviceMessage, Sink}
import karate.io.netty.channel._

class ScrcpyMessageHandler(device: String) extends SimpleChannelInboundHandler[DeviceMessage] with Sink[ControlMessage] {

  val receiverSinks = ReceiverHub.getSinks(device)
  var channel: Channel = null

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    channel = ctx.channel()
    ControllerHub.enter(device, this)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    super.channelInactive(ctx)
    ControllerHub.leave(device, this)
  }

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, msg: DeviceMessage): Unit = {
    ReceiverHub.write(receiverSinks, msg)
  }

  override def write(frame: ControlMessage): Boolean = {
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


