package asura.app.api

import akka.actor.ActorSystem
import akka.util.ByteString
import asura.app.AppErrorMessages
import asura.common.model.{ApiCode, ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{FieldKeys, Permissions}
import asura.core.es.service.{PermissionsService, UserProfileService}
import asura.core.model.QueryPermissions
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.mvc.{AnyContent, Request}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PermissionsApi @Inject()(
                                implicit val system: ActorSystem,
                                val exec: ExecutionContext,
                                val configuration: Configuration,
                                val controllerComponents: SecurityComponents,
                                val permissionAuthProvider: PermissionAuthProvider,
                              ) extends BaseApi {

  def roles() = Action.async { implicit req =>
    permissionAuthProvider.getUserRoles(getProfileId()).toOkResult
  }

  def putGroup(group: String) = Action(parse.byteString).async { implicit req =>
    put(group, None)
  }

  def deleteGroup(id: String, group: String) = Action.async { implicit req =>
    delete(id, group, None)
  }

  def updateGroup(id: String, group: String) = Action(parse.byteString).async { implicit req =>
    update(id, group, None)
  }

  def queryGroup(group: String) = Action(parse.byteString).async { implicit req =>
    query(group, None)
  }

  def putProject(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    put(group, Some(project))
  }

  def deleteProject(id: String, group: String, project: String) = Action.async { implicit req =>
    delete(id, group, Some(project))
  }

  def updateProject(id: String, group: String, project: String) = Action(parse.byteString).async { implicit req =>
    update(id, group, Some(project))
  }

  def queryProject(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    query(group, Some(project))
  }

  private def put(group: String, project: Option[String])(implicit req: Request[ByteString]) = {
    val doc = req.bodyAs(classOf[Permissions])
    val user = getProfileId()
    doc.fillCommonFields(user)
    setBasicFields(doc, group, project)
    PermissionsService.isExists(doc).flatMap(isExists => {
      if (!isExists) {
        isModifyAllowed(doc.role, user, group, project).flatMap(isAllowed => {
          if (isAllowed) {
            PermissionsService.index(doc).map(res => {
              toActionResultFromAny(res)
            })
          } else {
            Future.successful(OkApiRes(ApiRes(ApiCode.PERMISSION_DENIED, null)))
          }
        })
      } else {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_UserAlreadyJoined))))
      }
    })
  }

  def delete(id: String, group: String, project: Option[String])(implicit req: Request[AnyContent]) = {
    PermissionsService.getItemById(id).flatMap(item => {
      val projectCheck = if (project.nonEmpty) item.project.equals(project.get) else true
      if (item.group.equals(group) && projectCheck) {
        isModifyAllowed(item.role, getProfileId(), group, project).flatMap(isAllowed => {
          if (isAllowed) {
            PermissionsService.deleteDoc(id).toOkResult
          } else {
            Future.successful(OkApiRes(ApiRes(ApiCode.PERMISSION_DENIED, null)))
          }
        })
      } else {
        Future.successful(OkApiRes(ApiRes(ApiCode.PERMISSION_DENIED, null)))
      }
    })
  }

  def update(id: String, group: String, project: Option[String])(implicit req: Request[ByteString]) = {
    val doc = req.bodyAs(classOf[Permissions])
    setBasicFields(doc, group, project)
    isModifyAllowed(doc.role, getProfileId(), group, project).flatMap(isAllowed => {
      if (isAllowed) {
        PermissionsService.updateDoc(id, doc).toOkResult
      } else {
        Future.successful(OkApiRes(ApiRes(ApiCode.PERMISSION_DENIED, null)))
      }
    })
  }

  def query(group: String, project: Option[String])(implicit req: Request[ByteString]) = {
    checkPermission(group, project, if (project.isEmpty) Functions.GROUP_MEMBERS_VIEW else Functions.PROJECT_MEMBERS_VIEW) { _ =>
      val q = req.bodyAs(classOf[QueryPermissions])
      q.group = group
      if (project.isEmpty) {
        q.`type` = Permissions.TYPE_GROUP
      } else {
        q.project = project.get
        q.`type` = Permissions.TYPE_PROJECT
      }
      PermissionsService.queryDocs(q).flatMap(res => {
        val hits = res.result.hits
        val users = ArrayBuffer[String]()
        val list = hits.hits.map(hit => {
          val map = hit.sourceAsMap
          users += map.getOrElse(FieldKeys.FIELD_USERNAME, StringUtils.EMPTY).asInstanceOf[String]
          map + (FieldKeys.FIELD__ID -> hit.id)
        })
        UserProfileService.getByIdsAsRawMap(users).map(profiles => Map(
          "total" -> hits.total, "list" -> list, "profiles" -> profiles)
        )
      }).toOkResult
    }
  }

  private def isModifyAllowed(targetRole: String, user: String, group: String, project: Option[String]): Future[Boolean] = {
    permissionAuthProvider.getUserRoles(user).map(roles => {
      var isAllowed = false
      if (!roles.isAdmin) {
        val groupCheckFunc = () => {
          val item = roles.groups.get(group)
          if (null != item) item.role match {
            case Permissions.ROLE_OWNER =>
              isAllowed = true
            case Permissions.ROLE_MAINTAINER =>
              if (!Permissions.ROLE_OWNER.equals(targetRole)) isAllowed = true
            case _ =>
          }
        }
        if (project.nonEmpty) { // project
          if (!Permissions.ROLE_OWNER.equals(targetRole)) {
            groupCheckFunc()
            if (!isAllowed) {
              val item = roles.projects.get(Permissions.projectMapKey(group, project.get))
              if (null != item) item.role match {
                case Permissions.ROLE_MAINTAINER =>
                  isAllowed = true
                case _ =>
              }
            }
          }
        } else { // group
          groupCheckFunc()
        }
      } else {
        isAllowed = true
      }
      isAllowed
    })
  }

  private def setBasicFields(doc: Permissions, group: String, project: Option[String]): Unit = {
    doc.group = group
    if (project.nonEmpty) {
      doc.project = project.get
      doc.`type` = Permissions.TYPE_PROJECT
    } else {
      doc.project = null
      doc.`type` = Permissions.TYPE_GROUP
    }
  }
}
