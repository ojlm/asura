package asura.app.api

import java.util.Date

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.QueryJobState
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages.ErrorMessage
import asura.core.model.{AggsQuery, QueryJob, QueryJobReport}
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Activity
import asura.core.es.service._
import asura.core.job.actor._
import asura.core.job.{JobCenter, JobUtils, SchedulerManager}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import org.quartz.{CronExpression, TriggerKey}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobApi @Inject()(
                        implicit system: ActorSystem,
                        val exec: ExecutionContext,
                        val controllerComponents: SecurityComponents
                      ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def types() = Action {
    val jobTypes = JobCenter.supportedJobs.values.map(value => Map(
      "label" -> value.summary,
      "value" -> value.classAlias,
      "desc" -> value.description))
    OkApiRes(ApiRes(data = Map(
      "jobTypes" -> jobTypes, "schedulers" -> SchedulerManager.schedulers.keys()
    )))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[NewJob])
    val jobMeta = job.jobMeta
    val jobData = job.jobData
    val triggerMeta = job.triggerMeta
    val notifies = job.notifies
    var notifyError: ErrorMessage = null
    if (null != notifies) {
      for (i <- 0 until notifies.length if null == notifyError) {
        notifyError = JobNotifyService.validate(notifies(i), false)
      }
    }
    if (null == notifyError) {
      val error = JobUtils.validateJobAndTrigger(jobMeta, triggerMeta, jobData)
      if (null == error) {
        val user = getProfileId()
        SchedulerManager.scheduleJob(jobMeta, triggerMeta, jobData, notifies, user).map(res => {
          activityActor ! Activity(jobMeta.group, jobMeta.project, user, Activity.TYPE_NEW_JOB, res.id)
          toActionResultFromAny(res)
        })
      } else {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(error.name, error.errMsg))))
      }
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(notifyError.name, notifyError.errMsg))))
    }
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[UpdateJob])
    SchedulerManager.updateJob(job).map(res => {
      OkApiRes(ApiRes(msg = res))
    })
  }

  def pause() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[PauseJob])
    SchedulerManager.pauseJob(job).toOkResult
  }

  def resume() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[ResumeJob])
    SchedulerManager.resumeJob(job).toOkResult
  }

  def delete() = Action(parse.byteString).async { implicit req =>
    val job = req.bodyAs(classOf[DeleteJob])
    SchedulerManager.deleteJob(job).toOkResult
  }

  def detail(id: String) = Action(parse.byteString).async { implicit req =>
    JobService.getById(id).toOkResultByEsOneDoc(id)
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryJob])
    JobService.queryJob(query).toOkResultByEsList()
  }

  def reports() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryJobReport])
    JobReportService.query(query).toOkResultByEsList()
  }

  def report(id: String) = Action(parse.byteString).async { implicit req =>
    JobReportService.getById(id).toOkResultByEsOneDoc(id)
  }

  def reportItem(day: String, id: String) = Action(parse.byteString).async { implicit req =>
    JobReportDataService.getById(day, id).toOkResultByEsOneDoc(id)
  }

  def jobTrend(id: String, days: Option[Int]) = Action(parse.byteString).async { implicit req =>
    JobReportService.jobTrend(id, days.getOrElse(30)).toOkResultByEsList()
  }

  def trend() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    val res = for {
      trends <- JobReportService.trend(aggs)
    } yield trends
    res.map(trends => {
      OkApiRes(ApiRes(data = Map("trends" -> trends)))
    })
  }

  def cron() = Action(parse.tolerantText) { implicit req =>
    val cron = req.body
    if (StringUtils.isNotEmpty(cron) && CronExpression.isValidExpression(cron)) {
      val expression = new CronExpression(cron)
      val dates = ArrayBuffer[Date]()
      var now = new Date()
      do {
        now = expression.getNextValidTimeAfter(now)
        if (null != now) dates += now
      } while (dates.length < 5 && null != now)
      OkApiRes(ApiRes(data = dates))
    } else {
      OkApiRes(ApiRes(msg = getI18nMessage(AppErrorMessages.error_InvalidCronExpression)))
    }
  }

  def getJobState() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryJobState])
    if (null != query.items && query.items.nonEmpty) {
      val keys = query.items.map(item => TriggerKey.triggerKey(item.jobId, JobUtils.generateQuartzGroup(item.group, item.project)))
      SchedulerManager.getTriggerState(SchedulerManager.DEFAULT_SCHEDULER, keys).toOkResult
    } else {
      Future.successful(OkApiRes(ApiRes(data = Map.empty)))
    }
  }
}
