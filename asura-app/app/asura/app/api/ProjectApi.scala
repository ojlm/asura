package asura.app.api

import akka.actor.ActorSystem
import asura.common.model.{ApiRes, ApiResError}
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, FieldKeys, Permissions, Project}
import asura.core.es.service.{GroupService, PermissionsService, ProjectService}
import asura.core.model.{QueryProject, TransferProject}
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProjectApi @Inject()(
                            implicit val system: ActorSystem,
                            val exec: ExecutionContext,
                            val configuration: Configuration,
                            val controllerComponents: SecurityComponents,
                            val permissionAuthProvider: PermissionAuthProvider,
                          ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def get(group: String, project: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_INFO_VIEW) { _ =>
      ProjectService.getById(group, project).toOkResultByEsOneDoc(Project.generateDocId(group, project))
    }
  }

  def delete(group: String, project: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_REMOVE) { user =>
      ProjectService.deleteProject(group, project).map(res => {
        activityActor ! Activity(group, project, user, Activity.TYPE_DELETE_PROJECT, Project.generateDocId(group, project))
        res
      }).toOkResult
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_CREATE) { user =>
      val doc = req.bodyAs(classOf[Project])
      doc.group = group
      doc.id = project
      doc.fillCommonFields(user)
      ProjectService.index(doc).flatMap(res => {
        activityActor ! Activity(group, project, user, Activity.TYPE_NEW_PROJECT, res.id)
        PermissionsService.isGroupMaintainerOrOwner(group, user).flatMap(bRet => {
          if (bRet) {
            Future.successful(toActionResultFromAny(res))
          } else {
            val member = Permissions(
              group = group, project = project, `type` = Permissions.TYPE_PROJECT,
              username = user, role = Permissions.ROLE_MAINTAINER)
            member.fillCommonFields(user)
            PermissionsService.index(member).map(_ => toActionResultFromAny(res))
          }
        })
      })
    }
  }

  def update(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_INFO_EDIT) { _ =>
      val doc = req.bodyAs(classOf[Project])
      doc.group = group
      doc.id = project
      ProjectService.updateProject(doc).toOkResult
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.PROJECT_LIST) { _ =>
      val queryProject = req.bodyAs(classOf[QueryProject])
      ProjectService.queryProject(queryProject)
        .flatMap(esResponse => {
          if (esResponse.isSuccess) {
            val hits = esResponse.result.hits
            if (queryProject.includeGroup) {
              val groups = hits.hits.map(hit => hit.sourceAsMap.get(FieldKeys.FIELD_GROUP).get.asInstanceOf[String])
              GroupService.getByIdsAsRawMap(groups).map(groupMap => {
                OkApiRes(ApiRes(data = Map(
                  "total" -> hits.total.value, "list" -> hits.hits.map(hit => hit.sourceAsMap), "groups" -> groupMap
                )))
              })
            } else {
              Future.successful(OkApiRes(ApiRes(data = EsResponse.toApiData(esResponse.result, false))))
            }
          } else {
            Future.successful(OkApiRes(ApiResError(msg = esResponse.error.reason)))
          }
        })
    }
  }

  def getOpenApi(group: String, project: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_OPENAPI_VIEW) { _ =>
      ProjectService.getOpenApi(group, project).toOkResultByEsOneDoc(Project.generateDocId(group, project))
    }
  }

  def updateOpenApi(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_OPENAPI_EDIT) { _ =>
      val openapi = req.body.decodeString("UTF-8")
      ProjectService.updateOpenApi(group, project, openapi).toOkResult
    }
  }

  def transfer() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.PROJECT_TRANSFER) { _ =>
      val op = req.bodyAs(classOf[TransferProject])
      ProjectService.transferProject(op).toOkResult
    }
  }
}
