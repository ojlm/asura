package asura.app.api

import java.util.Date

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.QueryJobState
import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.Permissions.Functions
import asura.core.es.model.{Activity, JobTrigger}
import asura.core.es.service._
import asura.core.job.actor._
import asura.core.job.{JobCenter, JobUtils, SchedulerManager}
import asura.core.model.{AggsQuery, QueryJob, QueryJobReport}
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import org.quartz.{CronExpression, TriggerKey}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobApi @Inject()(
                        implicit system: ActorSystem,
                        val exec: ExecutionContext,
                        val controllerComponents: SecurityComponents,
                        val permissionAuthProvider: PermissionAuthProvider,
                      ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def types(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_TYPES) { _ =>
      val jobTypes = JobCenter.supportedJobs.values.map(value => Map(
        "label" -> value.summary,
        "value" -> value.classAlias,
        "desc" -> value.description))
      Future.successful(OkApiRes(ApiRes(data = Map(
        "jobTypes" -> jobTypes, "schedulers" -> SchedulerManager.schedulers.keys()
      ))))
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_EDIT) { user =>
      val job = req.bodyAs(classOf[NewJob])
      val jobMeta = job.jobMeta
      jobMeta.group = group
      jobMeta.project = project
      val jobData = job.jobData
      val triggerMeta = job.triggerMeta
      triggerMeta.group = group
      triggerMeta.project = project
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
          SchedulerManager.scheduleJob(jobMeta, triggerMeta, jobData, notifies, user, job.imports).map(res => {
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
  }

  def update(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_EDIT) { user =>
      val job = req.bodyAs(classOf[UpdateJob])
      if (null != job.jobMeta) {
        job.jobMeta.group = group
        job.jobMeta.project = project
      }
      if (null != job.triggerMeta) {
        job.triggerMeta.group = group
        job.triggerMeta.project = project
      }
      SchedulerManager.updateJob(job).map(res => {
        val jobMeta = job.jobMeta
        activityActor ! Activity(group, project, user, Activity.TYPE_UPDATE_JOB, job.id)
        OkApiRes(ApiRes(msg = res))
      })
    }
  }

  def pause(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_EDIT) { _ =>
      val job = req.bodyAs(classOf[PauseJob])
      job.group = group
      job.project = project
      SchedulerManager.pauseJob(job).toOkResult
    }
  }

  def resume(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_EDIT) { _ =>
      val job = req.bodyAs(classOf[ResumeJob])
      job.group = group
      job.project = project
      SchedulerManager.resumeJob(job).toOkResult
    }
  }

  def delete(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_EDIT) { _ =>
      val job = req.bodyAs(classOf[DeleteJob])
      job.group = group
      job.project = project
      SchedulerManager.deleteJob(job).toOkResult
    }
  }

  def detail(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_DETAIL) { _ =>
      JobService.getById(id).toOkResultByEsOneDoc(id)
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_LIST) { _ =>
      val query = req.bodyAs(classOf[QueryJob])
      query.group = group
      query.project = project
      JobService.queryJob(query).toOkResultByEsList()
    }
  }

  def reports(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_REPORT_LIST) { _ =>
      val query = req.bodyAs(classOf[QueryJobReport])
      JobReportService.query(query).toOkResultByEsList()
    }
  }

  def report(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_REPORT_DETAIL) { _ =>
      JobReportService.getById(id).toOkResultByEsOneDoc(id)
    }
  }

  def reportItem(group: String, project: String, day: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_REPORT_DETAIL) { _ =>
      JobReportDataItemService.getById(day, id).toOkResultByEsOneDoc(id)
    }
  }

  def jobTrend(group: String, project: String, id: String, days: Option[Int]) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_REPORT_TREND) { _ =>
      JobReportService.jobTrend(id, days.getOrElse(30)).toOkResultByEsList()
    }
  }

  def trend(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_REPORT_TREND) { _ =>
      val aggs = req.bodyAs(classOf[AggsQuery])
      val res = for {
        trends <- JobReportService.trend(aggs)
      } yield trends
      res.map(trends => {
        OkApiRes(ApiRes(data = Map("trends" -> trends)))
      })
    }
  }

  def cron(group: String, project: String) = Action(parse.tolerantText).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_DETAIL) { _ =>
      Future.successful {
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
    }
  }

  def getJobState(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_STATES) { _ =>
      val query = req.bodyAs(classOf[QueryJobState])
      if (null != query.items && query.items.nonEmpty) {
        val keys = query.items.map(item => TriggerKey.triggerKey(item.jobId, JobUtils.generateQuartzGroup(item.group, item.project)))
        SchedulerManager.getTriggerState(SchedulerManager.DEFAULT_SCHEDULER, keys).toOkResult
      } else {
        Future.successful(OkApiRes(ApiRes(data = Map.empty)))
      }
    }
  }

  def copyById(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.JOB_CLONE) { user =>
      JobService.getJobById(id).flatMap(job => {
        job.fillCommonFields(user)
        job.trigger = Seq(JobTrigger(job.group, job.project))
        JobService.index(job)
      }).toOkResult
    }
  }
}
