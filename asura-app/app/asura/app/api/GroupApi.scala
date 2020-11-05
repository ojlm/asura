package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.auth.Reserved
import asura.common.model.ApiResError
import asura.common.util.StringUtils
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, Group, Permissions}
import asura.core.es.service.{GroupService, JobService, PermissionsService, ProjectService}
import asura.core.model.{QueryGroup, QueryJob, QueryProject}
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupApi @Inject()(
                          implicit val system: ActorSystem,
                          val exec: ExecutionContext,
                          val configuration: Configuration,
                          val controllerComponents: SecurityComponents,
                          val permissionAuthProvider: PermissionAuthProvider,
                        ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    checkPermission(id, None, Functions.GROUP_INFO_VIEW) { _ =>
      GroupService.getById(id).toOkResultByEsOneDoc(id)
    }
  }

  def delete(id: String) = Action.async { implicit req =>
    checkPermission(id, None, Functions.GROUP_REMOVE) { user =>
      activityActor ! Activity(id, StringUtils.EMPTY, user, Activity.TYPE_DELETE_GROUP, id)
      GroupService.deleteGroup(id).toOkResult
    }
  }

  def put() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.GROUP_CREATE) { user =>
      val group = req.bodyAs(classOf[Group])
      group.fillCommonFields(user)
      if (Reserved.isReservedGroup(group.id)) {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CanNotUseReservedGroup))))
      } else {
        GroupService.index(group).flatMap(res => {
          activityActor ! Activity(group.id, StringUtils.EMPTY, user, Activity.TYPE_NEW_GROUP, group.id)
          val member = Permissions(
            group = group.id, project = null, `type` = Permissions.TYPE_GROUP,
            username = user, role = Permissions.ROLE_OWNER)
          member.fillCommonFields(user)
          PermissionsService.index(member).map(_ => toActionResultFromAny(res))
        })
      }
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.GROUP_LIST) { _ =>
      val queryGroup = req.bodyAs(classOf[QueryGroup])
      GroupService.queryGroup(queryGroup).toOkResultByEsList(false)
    }
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(id, None, Functions.GROUP_INFO_EDIT) { _ =>
      val group = req.bodyAs(classOf[Group])
      group.id = id
      GroupService.updateGroup(group).toOkResult
    }
  }

  def projects(id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(id, None, Functions.GROUP_PROJECT_LIST) { _ =>
      val q = req.bodyAs(classOf[QueryProject])
      q.group = id
      ProjectService.queryProject(q).toOkResultByEsList(false)
    }
  }

  def jobs(id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(id, None, Functions.GROUP_JOB_LIST) { _ =>
      val q = req.bodyAs(classOf[QueryJob])
      q.group = id
      JobService.queryJob(q).toOkResultByEsList()
    }
  }
}
