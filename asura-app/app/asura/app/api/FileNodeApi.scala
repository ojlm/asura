package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.{FilePath, NewFile, NewFolder}
import asura.common.util.{FileUtils, JsonUtils, StringUtils}
import asura.core.es.model.FileNode.FileNodeData
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{DocRef, FileNode}
import asura.core.es.service.FileNodeService
import asura.core.model.QueryFile
import asura.core.security.PermissionAuthProvider
import asura.core.store.{BlobStoreEngine, BlobStoreEngines, UploadParams}
import asura.ui.karate.{KarateFeatureSummary, KarateRunner}
import asura.ui.solopi.SoloPiModel
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class FileNodeApi @Inject()(
                             implicit val system: ActorSystem,
                             val exec: ExecutionContext,
                             val configuration: Configuration,
                             val controllerComponents: SecurityComponents,
                             val permissionAuthProvider: PermissionAuthProvider,
                           ) extends BaseApi {

  private val storeEngine: Option[BlobStoreEngine] = configuration
    .getOptional[String]("asura.store.file")
    .flatMap(name => BlobStoreEngines.get(name))

  def uploadFile(group: String, project: String) = Action(parse.multipartFormData(true)).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val fileOpt = req.body.file("file")
      val appOpt = req.body.dataParts.get("app")
      if (fileOpt.nonEmpty && appOpt.nonEmpty && FileNode.isSupportedApp(appOpt.get(0)) && storeEngine.nonEmpty) {
        var path: Seq[DocRef] = null
        val parentOpt = req.body.dataParts.get("parent")
        val pathOpt = req.body.dataParts.get("path")
        if (pathOpt.nonEmpty) {
          path = JsonUtils.parse(pathOpt.get(0), classOf[FilePath]).path
        }
        val app = appOpt.get(0)
        val file = fileOpt.get
        val name: String = file.filename
        var summary: String = null
        var desc: String = null
        var data: FileNodeData = null
        app match {
          case FileNode.APP_SOLOPI =>
            val soloPiModel = SoloPiModel.parse(FileUtils.toString(file.ref))
            summary = soloPiModel.caseName
            desc = soloPiModel.caseDesc
            data = FileNodeData(toDataMap(soloPiModel))
          case FileNode.APP_KARATE =>
            val featureModel = KarateRunner.parseFeatureSummary(FileUtils.toString(file.ref))
            summary = featureModel.name
            desc = featureModel.description
            data = FileNodeData(toDataMap(featureModel))
          case FileNode.APP_RAW =>
            data = FileNodeData()
        }
        val parent = if (parentOpt.nonEmpty) parentOpt.get(0) else null
        FileNodeService.fileExists(group, project, file.filename, parent).flatMap(res => {
          if (res.result.count < 1) {
            storeEngine.get.upload(UploadParams(file.filename, file.fileSize, file.contentType, file.ref.toPath))
              .flatMap(params => {
                data.blob = params
                val doc = FileNode(
                  group = group,
                  project = project,
                  summary = summary,
                  description = desc,
                  name = name,
                  `type` = FileNode.TYPE_FILE,
                  parent = parent,
                  path = path,
                  size = params.contentLength,
                  extension = FileUtils.getFileExtension(name),
                  app = app,
                  data = data,
                )
                doc.fillCommonFields(user)
                FileNodeService.index(doc).map(res => Map("id" -> res.id, "doc" -> doc))
              }).toOkResult
          } else {
            toI18nFutureErrorResult(AppErrorMessages.error_FileAlreadyExist)
          }
        })
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def get(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      FileNodeService.getById(id).toOkResultByEsOneDoc(id)
    }
  }

  def newFile(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val q = req.bodyAs(classOf[NewFile])
      if (q != null && FileNode.isNameLegal(q.name) && StringUtils.isNotEmpty(q.app)) {
        FileNodeService.fileExists(group, project, q.name, q.parent).flatMap(res => {
          if (res.result.count < 1) {
            val doc = FileNode(
              group = group,
              project = project,
              name = q.name,
              `type` = FileNode.TYPE_FILE,
              description = q.description,
              parent = if (StringUtils.isNotEmpty(q.parent)) q.parent else null,
              path = if (StringUtils.isNotEmpty(q.parent) && q.path != null && q.path.nonEmpty) q.path else null,
              app = q.app,
              data = FileNodeData(q.data),
            )
            doc.fillCommonFields(user)
            FileNodeService.index(doc).map(res => Map("id" -> res.id, "doc" -> doc)).toOkResult
          } else {
            toI18nFutureErrorResult(AppErrorMessages.error_FileAlreadyExist)
          }
        })
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def newFolder(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val q = req.bodyAs(classOf[NewFolder])
      if (q != null && FileNode.isNameLegal(q.name)) {
        FileNodeService.fileExists(group, project, q.name, q.parent).flatMap(res => {
          if (res.result.count < 1) {
            val doc = FileNode(
              group = group,
              project = project,
              `type` = FileNode.TYPE_FOLDER,
              name = q.name,
              description = q.description,
              parent = if (StringUtils.isNotEmpty(q.parent)) q.parent else null,
              path = if (StringUtils.isNotEmpty(q.parent) && q.path != null && q.path.nonEmpty) q.path else null,
            )
            doc.fillCommonFields(user)
            FileNodeService.index(doc).map(res => Map("id" -> res.id, "doc" -> doc)).toOkResult
          } else {
            toI18nFutureErrorResult(AppErrorMessages.error_FileAlreadyExist)
          }
        })
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = req.bodyAs(classOf[QueryFile])
      q.group = group
      q.project = q.project
      FileNodeService.queryDocs(q).toOkResultByEsList()
    }
  }

  private def toDataMap(model: SoloPiModel): Map[String, Any] = {
    Map(
      "gmtCreate" -> model.gmtCreate,
      "gmtModify" -> model.gmtModify,
      "targetAppLabel" -> model.targetAppLabel,
      "targetAppPackage" -> model.targetAppPackage,
    )
  }

  private def toDataMap(model: KarateFeatureSummary): Map[String, Any] = {
    Map(
      "lineCount" -> model.lineCount,
      "sectionCount" -> model.sectionCount,
    )
  }
}
