package asura.ui.cli.server

import asura.ui.cli.hub.Hubs.StreamHub
import asura.ui.cli.hub.StreamFrame
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.{ByteBuf, ByteBufUtil}
import karate.io.netty.channel._

class ScrcpyStreamHandler(device: String) extends SimpleChannelInboundHandler[ByteBuf] {

  private val sinks = StreamHub.getSinks(device)

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, buf: ByteBuf): Unit = {
    // The video stream contains raw packets, without time information. When we
    // record, we retrieve the timestamps separately, from a "meta" header
    // added by the server before each raw packet.
    //
    // The "meta" header length is 12 bytes:
    // [. . . . . . . .|. . . .]. . . . . . . . . . . . . . . ...
    //  <-------------> <-----> <-----------------------------...
    //        PTS        packet        raw packet
    //                    size
    //
    // It is followed by <packet_size> bytes containing the packet/frame.
    val frame = StreamFrame(buf.readLong(), buf.readInt(), ByteBufUtil.getBytes(buf))
    StreamHub.write(sinks, frame)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    super.channelInactive(ctx)
    ctx.channel().close()
    // TODO: clear sinks
  }

}

object ScrcpyStreamHandler {

  val logger = Logger(getClass)
  val NO_PTS = -1
  val SCRCPY_FRAME_HEADER_PTS_LENGTH = 8
  val SCRCPY_FRAME_HEADER_PACKET_SIZE_LENGTH = 4
  val SCRCPY_FRAME_HEADER_LENGTH = SCRCPY_FRAME_HEADER_PTS_LENGTH + SCRCPY_FRAME_HEADER_PACKET_SIZE_LENGTH

}
