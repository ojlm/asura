package asura.ui.cli.hub

import karate.io.netty.buffer.{ByteBuf, DefaultByteBufHolder}

case class StreamFrame(
                        pts: Long,
                        size: Int,
                        private val buf: ByteBuf,
                      ) extends DefaultByteBufHolder(buf)
