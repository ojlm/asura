package asura.ui.cli.server

import java.util

import asura.common.codec.KryoCodec
import asura.ui.cli.server.IndigoMessageCodec.{IndigoMessageDecoder, IndigoMessageEncoder}
import asura.ui.message.IndigoMessage
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.{ByteBuf, ByteBufUtil}
import karate.io.netty.channel._
import karate.io.netty.handler.codec.{ByteToMessageDecoder, MessageToMessageEncoder}

class IndigoMessageCodec() extends CombinedChannelDuplexHandler[IndigoMessageDecoder, IndigoMessageEncoder] {
  init(new IndigoMessageDecoder(), new IndigoMessageEncoder())
}

object IndigoMessageCodec {

  val logger = Logger(IndigoMessageCodec.getClass)

  class IndigoMessageDecoder extends ByteToMessageDecoder {
    override def decode(ctx: ChannelHandlerContext, buf: ByteBuf, list: util.List[AnyRef]): Unit = {
      val message = KryoCodec.fromBytes(ByteBufUtil.getBytes(buf), classOf[IndigoMessage])
      buf.skipBytes(buf.readableBytes())
      list.add(message)
    }
  }

  class IndigoMessageEncoder extends MessageToMessageEncoder[IndigoMessage] {
    override def encode(ctx: ChannelHandlerContext, msg: IndigoMessage, list: util.List[AnyRef]): Unit = {
      val bytes = KryoCodec.toBytes(msg)
      val buf = ctx.channel().alloc().heapBuffer(bytes.length + 4)
      buf.writeInt(bytes.length)
      buf.writeBytes(bytes)
      list.add(buf)
    }
  }

}
