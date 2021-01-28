package asura.app.api

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.RunTaskInBlob
import asura.common.util.{HttpUtils, StringUtils}
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.{LogEntryService, UiTaskReportService}
import asura.core.model.{QueryUiReport, SearchAfterLogEntry}
import asura.core.security.PermissionAuthProvider
import asura.core.store.{BlobStoreEngine, BlobStoreEngines}
import javax.inject.{Inject, Singleton}
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UiTaskApi @Inject()(
                           implicit val system: ActorSystem,
                           implicit val exec: ExecutionContext,
                           val configuration: Configuration,
                           val controllerComponents: SecurityComponents,
                           val client: HeaderClient,
                           val permissionAuthProvider: PermissionAuthProvider,
                         ) extends BaseApi {

  private val storeEngine: Option[BlobStoreEngine] = configuration
    .getOptional[String]("asura.store.active")
    .flatMap(name => BlobStoreEngines.get(name))

  def getReport(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      UiTaskReportService.getById(id).toOkResultByEsOneDoc(id)
    }
  }

  def queryReport(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = req.bodyAs(classOf[QueryUiReport])
      q.group = group
      q.project = q.project
      UiTaskReportService.queryDocs(q).toOkResultByEsList()
    }
  }

  def getLogs(group: String, project: String, reportId: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = req.bodyAs(classOf[SearchAfterLogEntry])
      q.group = group
      q.project = project
      q.reportId = reportId
      LogEntryService.searchFeed(q).toOkResult
    }
  }

  def runSolopi(group: String, project: String, reportId: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = req.bodyAs(classOf[RunTaskInBlob])
      if (StringUtils.isNotEmpty(q.key) && q.servers != null && q.servers.nonEmpty && storeEngine.nonEmpty) {
        storeEngine.get.readBytes(q.key).flatMap(bytes => {
          val bodyStr = new String(bytes, StandardCharsets.UTF_8)
          val futures = q.servers.map(server => {
            HttpUtils.postJson(s"http://${server.host}:${server.port}", bodyStr, classOf[String])
          })
          Future.sequence(futures).toOkResult
        })
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

}
