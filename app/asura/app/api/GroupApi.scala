package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiResError
import asura.common.util.StringUtils
import asura.core.cs.model.QueryGroup
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Group}
import asura.core.es.service.GroupService
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
                        ) extends BaseApi {

  val administrators = configuration.getOptional[Seq[String]]("asura.admin").getOrElse(Nil).toSet
  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    GroupService.getById(id).toOkResultByEsOneDoc(id)
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val user = getProfileId()
    val isAllowed = if (administrators.nonEmpty) administrators.contains(user) else true
    if (isAllowed) {
      val group = req.bodyAs(classOf[Group])
      group.fillCommonFields(user)
      GroupService.index(group).map(res => {
        activityActor ! Activity(group.id, StringUtils.EMPTY, user, Activity.TYPE_NEW_GROUP, group.id)
        toActionResultFromAny(res)
      })
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_NotAllowedContactAdministrator, administrators.mkString(",")))))
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryGroup = req.bodyAs(classOf[QueryGroup])
    GroupService.queryGroup(queryGroup).toOkResultByEsList(false)
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val group = req.bodyAs(classOf[Group])
    GroupService.updateGroup(group).toOkResult
  }
}
