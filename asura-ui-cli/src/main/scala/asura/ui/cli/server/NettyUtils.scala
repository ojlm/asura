package asura.ui.cli.server

import io.netty.karate.buffer.Unpooled
import io.netty.karate.channel.{Channel, ChannelFutureListener}

object NettyUtils {

  def flushAndClose(ch: Channel): Unit = {
    if (ch.isActive) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
  }

}
