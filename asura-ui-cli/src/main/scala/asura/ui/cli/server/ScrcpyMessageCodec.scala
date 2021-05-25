package asura.ui.cli.server

import java.nio.charset.StandardCharsets
import java.util

import asura.ui.cli.hub.{ControlMessage, DeviceMessage}
import asura.ui.cli.server.ScrcpyMessageCodec.{ScrcpyControlMessageEncoder, ScrcpyDeviceMessageDecoder}
import karate.io.netty.buffer.ByteBuf
import karate.io.netty.channel._
import karate.io.netty.handler.codec.{ByteToMessageDecoder, MessageToMessageEncoder}

class ScrcpyMessageCodec() extends CombinedChannelDuplexHandler[ScrcpyDeviceMessageDecoder, ScrcpyControlMessageEncoder] {
  init(new ScrcpyDeviceMessageDecoder(), new ScrcpyControlMessageEncoder())
}

object ScrcpyMessageCodec {

  class ScrcpyDeviceMessageDecoder extends ByteToMessageDecoder {
    override def decode(channelHandlerContext: ChannelHandlerContext, buf: ByteBuf, list: util.List[AnyRef]): Unit = {
      val typ = buf.readByte()
      if (DeviceMessage.TYPE_CLIPBOARD == typ) {
        val len = buf.readInt()
        val content = buf.readCharSequence(len, StandardCharsets.UTF_8).toString
        list.add(DeviceMessage.ofClipboard(content))
      }
    }
  }

  class ScrcpyControlMessageEncoder extends MessageToMessageEncoder[ControlMessage] {
    override def encode(channelHandlerContext: ChannelHandlerContext, msg: ControlMessage, list: util.List[AnyRef]): Unit = {
      // TODO: convert to buf
    }
  }

}
