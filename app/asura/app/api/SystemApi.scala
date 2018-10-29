package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiResError
import asura.core.ErrorMessages
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.service.{IndexService, JobReportDataService}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SystemApi @Inject()(
                           implicit val system: ActorSystem,
                           val exec: ExecutionContext,
                           val configuration: Configuration,
                           val controllerComponents: SecurityComponents,
                         ) extends BaseApi {

  val administrators = configuration.getOptional[Seq[String]]("asura.admin").getOrElse(Nil).toSet
  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getJobReportIndices() = Action.async { implicit req =>
    JobReportDataService.getIndices().map(res => {
      if (res.isSuccess) {
        toActionResultFromAny(res.result)
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  def deleteJobReportIndex(index: String) = Action.async { implicit req =>
    val user = getProfileId()
    val isAllowed = if (administrators.nonEmpty) administrators.contains(user) else true
    if (isAllowed) {
      IndexService.delIndex(Seq(index)).toOkResult
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_NotAllowedContactAdministrator, administrators.mkString(",")))))
    }
  }
}
