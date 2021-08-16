package asura.ui.cli.server

import java.nio.charset.StandardCharsets
import java.util

import asura.ui.cli.server.ScrcpyMessageCodec.{ScrcpyControlMessageEncoder, ScrcpyDeviceMessageDecoder}
import asura.ui.hub.ControlMessage.Position
import asura.ui.hub.{ControlMessage, DeviceMessage}
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.ByteBuf
import karate.io.netty.channel._
import karate.io.netty.handler.codec.{ByteToMessageDecoder, MessageToMessageEncoder}

class ScrcpyMessageCodec() extends CombinedChannelDuplexHandler[ScrcpyDeviceMessageDecoder, ScrcpyControlMessageEncoder] {
  init(new ScrcpyDeviceMessageDecoder(), new ScrcpyControlMessageEncoder())
}

object ScrcpyMessageCodec {

  val logger = Logger(ScrcpyMessageCodec.getClass)

  class ScrcpyDeviceMessageDecoder extends ByteToMessageDecoder {

    override def decode(ctx: ChannelHandlerContext, buf: ByteBuf, list: util.List[AnyRef]): Unit = {
      val typ = buf.readByte()
      if (DeviceMessage.TYPE_CLIPBOARD == typ) {
        val len = buf.readInt()
        val content = buf.readCharSequence(len, StandardCharsets.UTF_8).toString
        list.add(DeviceMessage.ofClipboard(content))
      }
    }

  }

  class ScrcpyControlMessageEncoder extends MessageToMessageEncoder[ControlMessage] {

    def writePosition(buf: ByteBuf, position: Position): Unit = {
      val point = position.point
      buf.writeInt(point.x)
      buf.writeInt(point.y)
      val screenSize = position.screenSize
      buf.writeShort(screenSize.width)
      buf.writeShort(screenSize.height)
    }

    override def encode(ctx: ChannelHandlerContext, msg: ControlMessage, list: util.List[AnyRef]): Unit = {
      msg.`type` match {
        case ControlMessage.TYPE_INJECT_KEYCODE =>
          val buf = ctx.alloc().heapBuffer(ControlMessage.INJECT_KEYCODE_LENGTH, ControlMessage.INJECT_KEYCODE_LENGTH)
          buf.writeByte(msg.`type`)
          buf.writeByte(msg.action)
          buf.writeInt(msg.keycode)
          buf.writeInt(msg.repeat)
          buf.writeInt(msg.metaState)
          list.add(buf)
        case ControlMessage.TYPE_INJECT_TOUCH_EVENT =>
          val buf = ctx.alloc().heapBuffer(ControlMessage.INJECT_TOUCH_EVENT_LENGTH, ControlMessage.INJECT_TOUCH_EVENT_LENGTH)
          buf.writeByte(msg.`type`)
          buf.writeByte(msg.action)
          buf.writeLong(msg.pointerId)
          writePosition(buf, msg.position)
          var pressure = (msg.pressure * 65536).toInt // 2^16
          if (pressure >= 0xFFFF) {
            pressure = 0xFFFF
          }
          buf.writeShort(pressure)
          buf.writeInt(msg.buttons)
          list.add(buf)
        case ControlMessage.TYPE_INJECT_SCROLL_EVENT =>
          val buf = ctx.alloc().heapBuffer(ControlMessage.INJECT_SCROLL_EVENT_LENGTH, ControlMessage.INJECT_SCROLL_EVENT_LENGTH)
          buf.writeByte(msg.`type`)
          writePosition(buf, msg.position)
          buf.writeInt(msg.hScroll)
          buf.writeInt(msg.vScroll)
          list.add(buf)
        case _ => logger.warn(s"Unsupported type: ${msg.`type`}")
      }
    }

  }

}
