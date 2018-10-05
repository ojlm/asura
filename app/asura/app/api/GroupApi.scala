package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.StringUtils
import asura.core.cs.model.QueryGroup
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Group}
import asura.core.es.service.GroupService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class GroupApi @Inject()(
                          implicit system: ActorSystem,
                          exec: ExecutionContext,
                          val controllerComponents: SecurityComponents,
                        ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    GroupService.getById(id).toOkResultByEsOneDoc(id)
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val group = req.bodyAs(classOf[Group])
    val user = getProfileId()
    group.fillCommonFields(user)
    GroupService.index(group).map(res => {
      activityActor ! Activity(group.id, StringUtils.EMPTY, user, Activity.TYPE_NEW_GROUP, group.id)
      toActionResultFromAny(res)
    })
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
