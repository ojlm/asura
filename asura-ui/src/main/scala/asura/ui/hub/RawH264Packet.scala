package asura.ui.hub

case class RawH264Packet(
                          pts: Long,
                          size: Int,
                          buf: Array[Byte],
                        )
