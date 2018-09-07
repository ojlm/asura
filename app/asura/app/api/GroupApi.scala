package asura.app.api

import asura.core.cs.model.QueryGroup
import asura.core.es.model.Group
import asura.core.es.service.GroupService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class GroupApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    GroupService.getById(id).toOkResultByEsOneDoc(id)
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val group = req.bodyAs(classOf[Group])
    group.fillCommonFields(getProfileId())
    GroupService.index(group).toOkResult
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
