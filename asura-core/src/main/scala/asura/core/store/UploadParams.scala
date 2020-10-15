package asura.core.store

import java.nio.file.Path

case class UploadParams(
                         fileName: String,
                         length: Long,
                         contentType: Option[String],
                         path: Path, // temporary file
                       )
