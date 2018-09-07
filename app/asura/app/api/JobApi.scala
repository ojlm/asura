package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.{ApiRes, ApiResError}
import asura.core.cs.model.QueryJobReport
import asura.core.es.service.{JobReportService, JobService}
import asura.core.job.actor._
import asura.core.job.{JobCenter, JobUtils, SchedulerManager}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def types() = Action {
    val jobTypes = JobCenter.supportedJobs.values.map(value => Map(
      "label" -> value.name,
      "value" -> value.classAlias,
      "desc" -> value.desc))
    OkApiRes(ApiRes(data = Map(
      "jobTypes" -> jobTypes, "schedulers" -> SchedulerManager.schedulers.keys()
    )))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[NewJob])
    val jobMeta = job.jobMeta
    val jobData = job.jobData
    val triggerMeta = job.triggerMeta
    val username = getProfileId()
    JobUtils.validateJobAndTrigger(jobMeta, triggerMeta, jobData) match {
      case (true, _) => SchedulerManager.scheduleJob(jobMeta, triggerMeta, jobData, username).toOkResult
      case (false, errMsg) => Future.successful(OkApiRes(ApiResError(errMsg)))
    }
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[UpdateJob])
    SchedulerManager.updateJob(job).map(res => {
      OkApiRes(ApiRes(msg = res))
    })
  }

  def pause() = Action(parse.byteString) { implicit req =>
    val job = req.bodyAs(classOf[PauseJob])
    SchedulerManager.pauseJob(job) match {
      case (true, _) => OkApiRes(ApiRes())
      case (false, errMsg) => OkApiRes(ApiResError(errMsg))
    }
  }

  def resume() = Action(parse.byteString) { implicit req =>
    val job = req.bodyAs(classOf[ResumeJob])
    SchedulerManager.resumeJob(job) match {
      case (true, _) => OkApiRes(ApiRes())
      case (false, errMsg) => OkApiRes(ApiResError(errMsg))
    }
  }

  def delete() = Action(parse.byteString) { implicit req =>
    val job = req.bodyAs(classOf[DeleteJob])
    SchedulerManager.deleteJob(job) match {
      case (true, _) => OkApiRes(ApiRes())
      case (false, errMsg) => OkApiRes(ApiResError(errMsg))
    }
  }

  def detail(id: String) = Action(parse.byteString).async { implicit req =>
    JobService.getById(id).toOkResultByEsOneDoc(id)
  }

  def reports() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryJobReport])
    JobReportService.query(query).toOkResultByEsList()
  }

  def report(id: String) = Action(parse.byteString).async { implicit req =>
    JobReportService.getById(id).toOkResultByEsOneDoc(id)
  }
}
