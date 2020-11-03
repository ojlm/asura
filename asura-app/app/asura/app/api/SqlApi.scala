package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.TestSql
import asura.common.model.ApiResError
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, ScenarioStep, SqlRequest}
import asura.core.es.service.{ScenarioService, SqlRequestService}
import asura.core.model.QuerySqlRequest
import asura.core.runtime.RuntimeContext
import asura.core.security.PermissionAuthProvider
import asura.core.sql.SqlRunner
import asura.core.util.{JacksonSupport, JsonPathUtils}
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SqlApi @Inject()(
                        implicit val system: ActorSystem,
                        val exec: ExecutionContext,
                        val configuration: Configuration,
                        val controllerComponents: SecurityComponents,
                        val permissionAuthProvider: PermissionAuthProvider,
                      ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def test(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EXEC) { user =>
      val testMsg = req.bodyAs(classOf[TestSql])
      val doc = testMsg.request
      val error = SqlRequestService.validate(doc)
      if (null == error) {
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_SQL, StringUtils.notEmptyElse(testMsg.id, StringUtils.EMPTY))
        val options = testMsg.options
        if (null != options && null != options.initCtx) {
          val initCtx = JsonPathUtils.parse(JacksonSupport.stringify(options.initCtx)).asInstanceOf[java.util.Map[Any, Any]]
          options.initCtx = initCtx
        }
        SqlRunner.test(testMsg.id, doc, RuntimeContext(options = options)).toOkResult
      } else {
        error.toFutureFail
      }
    }
  }

  def cloneRequest(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_CLONE) { user =>
      SqlRequestService.getRequestById(id).flatMap(doc => {
        doc.copyFrom = id
        doc.fillCommonFields(user)
        SqlRequestService.index(doc)
      }).toOkResult
    }
  }

  def getById(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      SqlRequestService.getById(id).flatMap(response => {
        withSingleUserProfile(id, response)
      })
    }
  }

  def delete(group: String, project: String, id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_REMOVE) { _ =>
      ScenarioService.containSteps(Seq(id), ScenarioStep.TYPE_SQL).flatMap(res => {
        if (res.isSuccess) {
          if (preview.nonEmpty && preview.get) {
            Future.successful(toActionResultFromAny(Map(
              "scenario" -> EsResponse.toApiData(res.result)
            )))
          } else {
            if (res.result.isEmpty) {
              SqlRequestService.deleteDoc(id).toOkResult
            } else {
              Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteCase))))
            }
          }
        } else {
          ErrorMessages.error_EsRequestFail(res).toFutureFail
        }
      })
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val doc = req.bodyAs(classOf[SqlRequest])
      doc.group = group
      doc.project = project
      doc.fillCommonFields(user)
      SqlRequestService.index(doc).map(res => {
        activityActor ! Activity(group, project, user, Activity.TYPE_NEW_SQL, res.id)
        toActionResultFromAny(res)
      })
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_LIST) { _ =>
      val q = req.bodyAs(classOf[QuerySqlRequest])
      SqlRequestService.query(q).toOkResult
    }
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val doc = req.bodyAs(classOf[SqlRequest])
      doc.group = group
      doc.project = project
      SqlRequestService.updateDoc(id, doc).map(res => {
        activityActor ! Activity(group, project, user, Activity.TYPE_UPDATE_SQL, id)
        res
      }).toOkResult
    }
  }

  def aggsLabels(group: String, project: String, label: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      SqlRequestService.aggsLabels(group, project, SqlRequest.Index, label).toOkResult
    }
  }
}
