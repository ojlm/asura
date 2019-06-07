package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.common.model.ApiResError
import asura.core.ErrorMessages
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.service.{IndexService, JobReportDataItemService}
import asura.core.job.SystemJobs
import asura.core.job.SystemJobs.{ClearJobReportIndicesJobModel, SyncDomainAndApiJobModel}
import asura.core.job.impl.{ClearJobReportDataIndicesJob, SyncOnlineDomainAndRestApiJob}
import asura.play.api.BaseApi.OkApiRes
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
    JobReportDataItemService.getIndices().map(res => {
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
      SystemJobs.pauseSystemJob(ClearJobReportDataIndicesJob.NAME).toOkResult
    }
  }

  def resumeClearJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      SystemJobs.resumeSystemJob(ClearJobReportDataIndicesJob.NAME).toOkResult
    }
  }

  def getSyncDomainAndApiJobDetail() = Action(parse.byteString).async { implicit req =>
    SystemJobs.getSyncDomainAndApiJob().toOkResult
  }

  def updateSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      val job = req.bodyAs(classOf[SyncDomainAndApiJobModel])
      SystemJobs.putOrUpdateSyncDomainApiJob(job).toOkResult
    }
  }

  def pauseSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      SystemJobs.pauseSystemJob(SyncOnlineDomainAndRestApiJob.NAME).toOkResult
    }
  }

  def resumeSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPrivilege {
      SystemJobs.resumeSystemJob(SyncOnlineDomainAndRestApiJob.NAME).toOkResult
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
