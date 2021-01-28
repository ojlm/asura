package asura.app.api

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.common.model.ApiRes
import asura.core.es.model.Permissions.Functions
import asura.core.security.PermissionAuthProvider
import asura.core.store.{BlobStoreEngine, BlobStoreEngines, UploadParams}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData

import scala.concurrent.ExecutionContext

@Singleton
class BlobApi @Inject()(
                         implicit val system: ActorSystem,
                         val exec: ExecutionContext,
                         val configuration: Configuration,
                         val controllerComponents: SecurityComponents,
                         val permissionAuthProvider: PermissionAuthProvider,
                       ) extends BaseApi {

  private val storeEngine: Option[BlobStoreEngine] = configuration
    .getOptional[String]("asura.store.active")
    .flatMap(name => BlobStoreEngines.get(name))

  def uploadBlob(group: String, project: String) = Action(parse.multipartFormData(true)).async { implicit req =>
    checkPermission(group, Some(project), Functions.BLOB_UPLOAD) { _ =>
      val fileParam = req.body.file("file")
      if (fileParam.nonEmpty) {
        saveBlob(fileParam.get).toOkResult
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_FileNotExist.name)
      }
    }
  }

  def readAsString(group: String, project: String, key: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.BLOB_DOWNLOAD) { _ =>
      if (storeEngine.nonEmpty) {
        storeEngine.get.readBytes(key).map(bytes => {
          ApiRes(data = new String(bytes, StandardCharsets.UTF_8))
        }).toOkResult
      } else {
        AppErrorMessages.error_NonActiveStoreEngine.toFutureFail
      }
    }
  }

  def downloadBlob(group: String, project: String, key: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.BLOB_DOWNLOAD) { _ =>
      if (storeEngine.nonEmpty) {
        storeEngine.get.download(key).map(params => {
          val playEntity = HttpEntity.Streamed(params.source, params.length, None)
          Ok.sendEntity(playEntity, false, None)
        })
      } else {
        AppErrorMessages.error_NonActiveStoreEngine.toFutureFail
      }
    }
  }

  private def saveBlob(upFile: MultipartFormData.FilePart[TemporaryFile]) = {
    if (storeEngine.nonEmpty) {
      storeEngine.get.upload(UploadParams(upFile.filename, upFile.fileSize, upFile.contentType, upFile.ref.toPath))
    } else {
      AppErrorMessages.error_NonActiveStoreEngine.toFutureFail
    }
  }
}
