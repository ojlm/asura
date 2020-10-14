package asura.core.store

import java.nio.file.Path

case class UploadParams(
                         fileName: String,
                         length: Long,
                         path: Path, // temporary file
                       )
