package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
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
                         val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  private val storeEngine: Option[BlobStoreEngine] = configuration
    .getOptional[String]("asura.store.active")
    .flatMap(name => BlobStoreEngines.get(name))

  def uploadBlob() = Action(parse.multipartFormData(true)).async { implicit req =>
    val fileParam = req.body.file("file")
    if (fileParam.nonEmpty) {
      saveBlob(fileParam.get).toOkResult
    } else {
      toI18nFutureErrorResult(AppErrorMessages.error_FileNotExist.name)
    }
  }

  def downloadBlob(key: String) = Action.async { implicit req =>
    if (storeEngine.nonEmpty) {
      storeEngine.get.download(key).map(params => {
        val playEntity = HttpEntity.Streamed(params.source, params.length, None)
        Ok.sendEntity(playEntity, false, None)
      })
    } else {
      AppErrorMessages.error_NonActiveStoreEngine.toFutureFail
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
