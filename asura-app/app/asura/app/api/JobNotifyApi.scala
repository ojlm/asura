package asura.app.api

import akka.actor.ActorSystem
import asura.common.model.ApiRes
import asura.core.es.model.JobNotify
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.JobNotifyService
import asura.core.model.QueryJobNotify
import asura.core.notify.JobNotifyManager
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobNotifyApi @Inject()(
                              implicit system: ActorSystem,
                              val exec: ExecutionContext,
                              val controllerComponents: SecurityComponents,
                              val permissionAuthProvider: PermissionAuthProvider,
                            ) extends BaseApi {

  def all(group: String, project: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_NOTIFY_VIEW) { _ =>
      Future.successful(OkApiRes(ApiRes(data = JobNotifyManager.all())))
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_NOTIFY_EDIT) { user =>
      val doc = req.bodyAs(classOf[JobNotify])
      doc.fillCommonFields(user)
      doc.group = group
      doc.project = project
      JobNotifyService.index(doc).toOkResult
    }
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_NOTIFY_EDIT) { _ =>
      val doc = req.bodyAs(classOf[JobNotify])
      doc.group = group
      doc.project = project
      JobNotifyService.updateNotify(id, doc).toOkResult
    }
  }

  def delete(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_NOTIFY_EDIT) { _ =>
      JobNotifyService.deleteDoc(id).toOkResult
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_NOTIFY_VIEW) { _ =>
      val query = req.bodyAs(classOf[QueryJobNotify])
      query.group = group
      query.project = project
      JobNotifyService.querySubscribers(query).toOkResultByEsList(true)
    }
  }
}
