package asura.app.api

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.pattern.ask
import asura.app.AppErrorMessages
import asura.app.api.model.RunTaskInBlob
import asura.common.util.{HttpUtils, StringUtils}
import asura.core.es.actor.UiTaskListenerActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.{LogEntryService, UiTaskReportService}
import asura.core.model.{QueryUiReport, SearchAfterLogEntry}
import asura.core.security.PermissionAuthProvider
import asura.core.store.{BlobStoreEngine, BlobStoreEngines}
import asura.ui.UiConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.ui.actor.ServosTaskControllerActor
import asura.ui.driver.{CommandMeta, DriverCommand}
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
    .getOptional[String]("asura.store.file")
    .flatMap(name => BlobStoreEngines.get(name))
  private val imageStoreEngine: Option[BlobStoreEngine] = configuration
    .getOptional[String]("asura.store.image")
    .flatMap(name => BlobStoreEngines.get(name))
  val taskListener = system.actorOf(UiTaskListenerActor.props(imageStoreEngine))

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

  def getAggs(group: String, project: String, reportId: String, day: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val q = SearchAfterLogEntry()
      q.group = group
      q.project = project
      q.reportId = reportId
      q.day = day
      LogEntryService.getAggs(q).toOkResult
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

  def runSolopi(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { _ =>
      val q = req.bodyAs(classOf[RunTaskInBlob])
      if (StringUtils.isNotEmpty(q.key) && q.servos != null && q.servos.nonEmpty && storeEngine.nonEmpty) {
        storeEngine.get.readBytes(q.key).flatMap(bytes => {
          val bodyStr = new String(bytes, StandardCharsets.UTF_8)
          val futures = q.servos.map(servo => {
            HttpUtils.postJson(s"http://${servo.host}:${servo.port}", bodyStr, classOf[String])
          })
          Future.sequence(futures).toOkResult
        })
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

  def runCommand(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val q = req.bodyAs(classOf[DriverCommand])
      if (q.validateServos() && storeEngine.nonEmpty) {
        q.meta = CommandMeta(group = group, project = project, taskId = id, creator = user)
        val controller = system.actorOf(ServosTaskControllerActor.props(q, taskListener))
        (controller ? ServosTaskControllerActor.Start).toOkResult
      } else {
        toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
      }
    }
  }

}
