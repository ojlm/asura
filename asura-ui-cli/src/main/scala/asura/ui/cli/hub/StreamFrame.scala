package asura.ui.cli.hub

case class StreamFrame(
                        pts: Long,
                        size: Int,
                        buf: Array[Byte],
                      )
