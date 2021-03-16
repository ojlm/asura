package asura.app.store

import java.io.{BufferedOutputStream, File, FileOutputStream}
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
    check((key, toFile) => {
      Files.move(params.path, toFile.toPath).toFile
      Future.successful(BlobMetaData(name, key, params.fileName, params.length, params.contentType.getOrElse(null)))
    })
  }

  override def uploadBytes(bytes: Array[Byte]): Future[BlobMetaData] = {
    check((key, toFile) => {
      val bos = new BufferedOutputStream(new FileOutputStream(toFile))
      bos.write(bytes)
      bos.close()
      Future.successful(BlobMetaData(name, key, null, bytes.length, null))
    })
  }

  private def check(func: (String, File) => Future[BlobMetaData]): Future[BlobMetaData] = {
    if (storeDir.nonEmpty) {
      val key = UUID.randomUUID().toString
      val toFile = new File(s"${storeDir.get}${File.separator}${key}")
      if (toFile.getCanonicalPath.startsWith(storeDir.get)) {
        func(key, toFile)
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

  override def readBytes(key: String): Future[Array[Byte]] = {
    val file = new File(s"${storeDir.get}${File.separator}${key}")
    if (file.exists()) {
      Future.successful(Files.readAllBytes(file.toPath))
    } else {
      AppErrorMessages.error_FileNotExist.toFutureFail
    }
  }

}
