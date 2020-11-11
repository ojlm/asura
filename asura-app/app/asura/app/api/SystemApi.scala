package asura.app.api

import akka.actor.ActorSystem
import asura.core.ErrorMessages
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.{IndexService, JobReportDataItemService}
import asura.core.job.SystemJobs
import asura.core.job.SystemJobs.{ClearJobReportIndicesJobModel, SyncDomainAndApiJobModel}
import asura.core.job.impl.{ClearJobReportDataIndicesJob, SyncOnlineDomainAndRestApiJob}
import asura.core.security.PermissionAuthProvider
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class SystemApi @Inject()(
                           implicit val system: ActorSystem,
                           val exec: ExecutionContext,
                           val configuration: Configuration,
                           val controllerComponents: SecurityComponents,
                           val permissionAuthProvider: PermissionAuthProvider,
                         ) extends BaseApi {


  def getJobReportIndices() = Action.async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_ES_VIEW) { _ =>
      JobReportDataItemService.getIndices().map(res => {
        if (res.isSuccess) {
          toActionResultFromAny(res.result)
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    }
  }

  def deleteJobReportIndex(index: String) = Action.async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_ES_EDIT) { _ =>
      IndexService.delIndex(Seq(index)).toOkResult
    }
  }

  def getClearJobDetail() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_VIEW) { _ =>
      SystemJobs.getClearReportIndicesJob().toOkResult
    }
  }

  def updateClearJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      val job = req.bodyAs(classOf[ClearJobReportIndicesJobModel])
      SystemJobs.putOrUpdateClearJobReportIndicesJob(job).toOkResult
    }
  }

  def pauseClearJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      SystemJobs.pauseSystemJob(ClearJobReportDataIndicesJob.NAME).toOkResult
    }
  }

  def resumeClearJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      SystemJobs.resumeSystemJob(ClearJobReportDataIndicesJob.NAME).toOkResult
    }
  }

  def getSyncDomainAndApiJobDetail() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_VIEW) { _ =>
      SystemJobs.getSyncDomainAndApiJob().toOkResult
    }
  }

  def updateSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      val job = req.bodyAs(classOf[SyncDomainAndApiJobModel])
      SystemJobs.putOrUpdateSyncDomainApiJob(job).toOkResult
    }
  }

  def pauseSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      SystemJobs.pauseSystemJob(SyncOnlineDomainAndRestApiJob.NAME).toOkResult
    }
  }

  def resumeSyncDomainAndApiJob() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.SYSTEM_JOBS_EDIT) { _ =>
      SystemJobs.resumeSystemJob(SyncOnlineDomainAndRestApiJob.NAME).toOkResult
    }
  }
}
