package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiResError
import asura.core.ErrorMessages
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.service.{IndexService, JobReportDataService}
import asura.core.job.SystemJobs
import asura.core.job.SystemJobs.ClearJobReportIndicesJobModel
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result}

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
    checkPrivilege {
      IndexService.delIndex(Seq(index)).toOkResult
    }
  }

  def getClearJobDetail() = Action(parse.byteString).async { implicit req =>
    SystemJobs.getClearReportIndicesJob().toOkResult
  }

  def updateClearJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      val job = req.bodyAs(classOf[ClearJobReportIndicesJobModel])
      SystemJobs.putOrUpdateClearJobReportIndicesJob(job).toOkResult
    }
  }

  def pauseClearJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      SystemJobs.pauseClearReportIndicesJob().toOkResult
    }
  }

  def resumeClearJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      SystemJobs.resumeClearReportIndicesJob().toOkResult
    }
  }

  private def checkPrivilege(func: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    val user = getProfileId()
    val isAllowed = if (administrators.nonEmpty) administrators.contains(user) else true
    if (isAllowed) {
      func
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_NotAllowedContactAdministrator, administrators.mkString(",")))))
    }
  }
}
