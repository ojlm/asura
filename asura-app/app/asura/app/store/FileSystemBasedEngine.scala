package asura.app.store

import java.io.File
import java.nio.file.Files
import java.util.UUID

import akka.stream.scaladsl.FileIO
import asura.app.AppErrorMessages
import asura.core.es.model.FormDataItem.BlobMetaData
import asura.core.store.{BlobStoreEngine, DownloadParams, UploadParams}
import play.api.Configuration

import scala.concurrent.Future

case class FileSystemBasedEngine(config: Configuration) extends BlobStoreEngine {

  val storeDir = config.getOptional[String]("asura.store.local.dir").map(dir => new File(dir).getCanonicalPath)

  override val name: String = "local"
  override val description: String =
    """ Local file system
      |""".stripMargin

  override def upload(params: UploadParams): Future[BlobMetaData] = {
    if (storeDir.nonEmpty) {
      val key = UUID.randomUUID().toString
      val toFile = new File(s"${storeDir.get}${File.separator}${key}")
      if (toFile.getCanonicalPath.startsWith(storeDir.get)) {
        Files.move(params.path, toFile.toPath).toFile
        Future.successful(BlobMetaData(name, key, params.fileName, params.length))
      } else {
        AppErrorMessages.error_AccessDenied.toFutureFail
      }
    } else {
      AppErrorMessages.error_EmptyBlobStoreDir.toFutureFail
    }
  }

  override def download(key: String): Future[DownloadParams] = {
    val file = new File(s"${storeDir.get}${File.separator}${key}")
    if (file.exists()) {
      val source = FileIO.fromPath(file.toPath)
      Future.successful(DownloadParams(source, Some(file.length())))
    } else {
      AppErrorMessages.error_FileNotExist.toFutureFail
    }
  }
}
