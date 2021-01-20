package asura.app.api

import akka.actor.ActorSystem
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.{LogEntryService, UiTaskReportService}
import asura.core.model.{QueryUiReport, SearchAfterLogEntry}
import asura.core.security.PermissionAuthProvider
import javax.inject.{Inject, Singleton}
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class UiTaskApi @Inject()(
                           implicit val system: ActorSystem,
                           implicit val exec: ExecutionContext,
                           val controllerComponents: SecurityComponents,
                           val client: HeaderClient,
                           val permissionAuthProvider: PermissionAuthProvider,
                         ) extends BaseApi {

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

}
