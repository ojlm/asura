package asura.app.api

import akka.actor.ActorSystem
import akka.util.ByteString
import asura.common.util.StringUtils
import asura.core.es.model.{FieldKeys, Permissions}
import asura.core.es.service.{PermissionsService, UserProfileService}
import asura.core.model.QueryPermissions
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.mvc.Request

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

@Singleton
class PermissionsApi @Inject()(
                                implicit val system: ActorSystem,
                                val exec: ExecutionContext,
                                val configuration: Configuration,
                                val controllerComponents: SecurityComponents,
                              ) extends BaseApi {

  private def put(group: String, project: Option[String])(implicit req: Request[ByteString]) = {
    val doc = req.bodyAs(classOf[Permissions])
    doc.fillCommonFields(getProfileId())
    doc.group = group
    if (project.nonEmpty) {
      doc.project = project.get
      doc.`type` = Permissions.TYPE_PROJECT
    } else {
      doc.`type` = Permissions.TYPE_GROUP
    }
    PermissionsService.index(doc).map(res => {
      toActionResultFromAny(res)
    })
  }

  def delete(id: String, group: String, project: Option[String]) = {
    PermissionsService.deleteDoc(id).toOkResult
  }

  def update(id: String, group: String, project: Option[String])(implicit req: Request[ByteString]) = {
    val doc = req.bodyAs(classOf[Permissions])
    PermissionsService.updateDoc(id, doc).toOkResult
  }

  def query(group: String, project: Option[String])(implicit req: Request[ByteString]) = {
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
        map
      })
      UserProfileService.getByIdsAsRawMap(users).map(profiles => Map(
        "total" -> hits.total.value, "list" -> list, "profiles" -> profiles)
      )
    }).toOkResult
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
}
