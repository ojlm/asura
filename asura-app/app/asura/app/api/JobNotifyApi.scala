package asura.app.api

import akka.actor.ActorSystem
import asura.common.model.ApiRes
import asura.core.es.model.JobNotify
import asura.core.es.service.JobNotifyService
import asura.core.model.QueryJobNotify
import asura.core.notify.JobNotifyManager
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class JobNotifyApi @Inject()(
                              implicit system: ActorSystem,
                              exec: ExecutionContext,
                              val controllerComponents: SecurityComponents,
                            ) extends BaseApi {

  def all() = Action {
    OkApiRes(ApiRes(data = JobNotifyManager.all()))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val notify = req.bodyAs(classOf[JobNotify])
    val user = getProfileId()
    notify.fillCommonFields(user)
    JobNotifyService.index(notify).toOkResult
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val notify = req.bodyAs(classOf[JobNotify])
    JobNotifyService.updateNotify(id, notify).toOkResult
  }

  def delete(id: String) = Action(parse.byteString).async { implicit req =>
    JobNotifyService.deleteDoc(id).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryNotify = req.bodyAs(classOf[QueryJobNotify])
    JobNotifyService.querySubscribers(queryNotify).toOkResultByEsList(true)
  }
}
