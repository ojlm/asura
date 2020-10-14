package asura.core.store

import akka.stream.scaladsl.Source
import akka.util.ByteString

case class DownloadParams(
                           source: Source[ByteString, _],
                           length: Option[Long],
                         )
