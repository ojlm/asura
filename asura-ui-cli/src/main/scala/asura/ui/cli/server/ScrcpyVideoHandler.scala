package asura.ui.cli.server

import asura.ui.cli.codec.{Size, VideoStream}
import asura.ui.cli.hub.Hubs.{RawH264StreamHub, RenderingFrameHub}
import asura.ui.cli.hub.RawH264Packet
import asura.ui.cli.server.ScrcpyVideoHandler.logger
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.{ByteBuf, ByteBufUtil}
import karate.io.netty.channel._

class ScrcpyVideoHandler(device: String, width: Int, height: Int) extends SimpleChannelInboundHandler[ByteBuf] {

  private val stream = VideoStream.startThread(device)
  private val sinks = RawH264StreamHub.getSinks(device)

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
    val packet = RawH264Packet(buf.readLong(), buf.readInt(), ByteBufUtil.getBytes(buf))
    // TODO: reuse the ByteBuf
    stream.put(packet)
    RawH264StreamHub.write(sinks, packet)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    logger.info(s"$device: Screen is online")
    RenderingFrameHub.active(device, Size(width, height))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    stream.stop()
    RawH264StreamHub.closeAndRemoveSinks(device)
    super.channelInactive(ctx)
    ctx.channel().close()
  }

}

object ScrcpyVideoHandler {

  val logger = Logger(getClass)
  val NO_PTS = -1
  val SCRCPY_FRAME_HEADER_PTS_LENGTH = 8
  val SCRCPY_FRAME_HEADER_PACKET_SIZE_LENGTH = 4
  val SCRCPY_FRAME_HEADER_LENGTH = SCRCPY_FRAME_HEADER_PTS_LENGTH + SCRCPY_FRAME_HEADER_PACKET_SIZE_LENGTH

}
