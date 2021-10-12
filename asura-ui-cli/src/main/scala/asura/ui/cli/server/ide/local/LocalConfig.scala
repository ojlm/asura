package asura.ui.cli.server.ide.local

import java.nio.file.Path

case class LocalConfig(data: Path) {

  val PATH_ACTIVITY = data.resolve("activity")
  val PATH_USER: Path = data.resolve("user")
  val PATH_WORKSPACE: Path = data.resolve("workspace")
  val PATH_PROJECT: Path = data.resolve("project")
  val PATH_TREE: Path = data.resolve("tree")
  val PATH_BLOB: Path = data.resolve("blob")

  def validate(): Boolean = {
    val file = data.toFile
    if (file.exists()) {
      if (file.isDirectory) {
        true
      } else {
        throw new RuntimeException(s"${file.getAbsolutePath} is not a directory")
      }
    } else {
      if (file.mkdir()) {
        true
      } else {
        throw new RuntimeException(s"Can not create data dir: ${file.getAbsolutePath}")
      }
    }
  }

}
