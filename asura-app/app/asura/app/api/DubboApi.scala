package asura.app.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asura.app.AppErrorMessages
import asura.app.api.model.TestDubbo
import asura.common.model.ApiResError
import asura.common.util.StringUtils
import asura.core.dubbo.DubboRunner
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, DubboRequest, ScenarioStep}
import asura.core.es.service.{DubboRequestService, ScenarioService}
import asura.core.model.QueryDubboRequest
import asura.core.runtime.RuntimeContext
import asura.core.security.PermissionAuthProvider
import asura.core.util.{JacksonSupport, JsonPathUtils}
import asura.core.{CoreConfig, ErrorMessages, RunnerActors}
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfaceMethodParams, GetInterfacesMessage, GetProvidersMessage}
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DubboApi @Inject()(
                          implicit val system: ActorSystem,
                          val exec: ExecutionContext,
                          val configuration: Configuration,
                          val controllerComponents: SecurityComponents,
                          val permissionAuthProvider: PermissionAuthProvider,
                        ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
  lazy val dubboInvoker = RunnerActors.dubboInvoker

  def getInterfaces(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val msg = req.bodyAs(classOf[GetInterfacesMessage])
      (dubboInvoker ? msg).toOkResult
    }
  }

  def getProviders(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val msg = req.bodyAs(classOf[GetProvidersMessage])
      (dubboInvoker ? msg).toOkResult
    }
  }

  def getParams(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      val msg = req.bodyAs(classOf[GetInterfaceMethodParams])
      (dubboInvoker ? msg).toOkResult
    }
  }

  def test(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EXEC) { user =>
      val testMsg = req.bodyAs(classOf[TestDubbo])
      val doc = testMsg.request
      val error = DubboRequestService.validate(doc)
      if (null == error) {
        activityActor ! Activity(group, project, user, Activity.TYPE_TEST_DUBBO, StringUtils.notEmptyElse(testMsg.id, StringUtils.EMPTY))
        val options = testMsg.options
        if (null != options && null != options.initCtx) {
          val initCtx = JsonPathUtils.parse(JacksonSupport.stringify(options.initCtx)).asInstanceOf[java.util.Map[Any, Any]]
          options.initCtx = initCtx
        }
        DubboRunner.test(testMsg.id, testMsg.request, RuntimeContext(options = options)).toOkResult
      } else {
        error.toFutureFail
      }
    }
  }

  def cloneRequest(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_CLONE) { user =>
      DubboRequestService.getRequestById(id).flatMap(doc => {
        doc.copyFrom = id
        doc.fillCommonFields(user)
        DubboRequestService.index(doc)
      }).toOkResult
    }
  }

  def getById(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      DubboRequestService.getById(id).flatMap(response => {
        withSingleUserProfile(id, response)
      })
    }
  }

  def delete(group: String, project: String, id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_REMOVE) { _ =>
      ScenarioService.containSteps(Seq(id), ScenarioStep.TYPE_DUBBO).flatMap(res => {
        if (res.isSuccess) {
          if (preview.nonEmpty && preview.get) {
            Future.successful(toActionResultFromAny(Map(
              "scenario" -> EsResponse.toApiData(res.result)
            )))
          } else {
            if (res.result.isEmpty) {
              DubboRequestService.deleteDoc(id).toOkResult
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
      val doc = req.bodyAs(classOf[DubboRequest])
      doc.group = group
      doc.project = project
      doc.fillCommonFields(user)
      DubboRequestService.index(doc).map(res => {
        activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_NEW_DUBBO, res.id)
        toActionResultFromAny(res)
      })
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_LIST) { _ =>
      val q = req.bodyAs(classOf[QueryDubboRequest])
      DubboRequestService.query(q).toOkResult
    }
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val doc = req.bodyAs(classOf[DubboRequest])
      doc.group = group
      doc.project = project
      DubboRequestService.updateDoc(id, doc).map(res => {
        activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_UPDATE_DUBBO, id)
        res
      }).toOkResult
    }
  }

  def aggsLabels(group: String, project: String, label: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      DubboRequestService.aggsLabels(group, project, DubboRequest.Index, label).toOkResult
    }
  }
}
