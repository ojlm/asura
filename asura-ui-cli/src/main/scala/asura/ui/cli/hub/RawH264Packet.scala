package asura.ui.cli.hub

case class RawH264Packet(
                          pts: Long,
                          size: Int,
                          buf: Array[Byte],
                        )
