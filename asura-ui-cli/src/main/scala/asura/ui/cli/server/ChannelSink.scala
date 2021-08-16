package asura.ui.cli.server

import asura.ui.hub.Sink
import karate.io.netty.channel.Channel

abstract class ChannelSink[T](chn: Channel) extends Sink[T] {
  override def close(): Unit = {
    if (chn.isActive) {
      chn.close()
    }
  }
}
