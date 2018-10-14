package asura.core.job

import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import asura.common.util.{DateUtils, LogUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.es.model._
import asura.core.es.service.{JobNotifyService, JobService}
import asura.core.job.actor.{JobActionValidator, _}
import com.typesafe.scalalogging.Logger
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.triggers.{CronTriggerImpl, SimpleTriggerImpl}
import org.quartz.{JobKey, Scheduler, TriggerKey}

import scala.concurrent.Future

object SchedulerManager {

  /** use the first scheduler as the default one */
  var DEFAULT_SCHEDULER: String = null
  val logger = Logger("SchedulerManager")
  val schedulers = new ConcurrentHashMap[String, Scheduler]()

  def init(props: Properties*): Unit = {
    if (null != props && props.nonEmpty) {
      props.foreach(prop => {
        val scheduler = new StdSchedulerFactory(prop).getScheduler
        val name = scheduler.getSchedulerName
        if (null == DEFAULT_SCHEDULER) {
          DEFAULT_SCHEDULER = name
        }
        schedulers.put(name, scheduler)
        scheduler.getListenerManager.addSchedulerListener(NamedSchedulerListener(name))
        scheduler.start()
      })
    }
  }

  def shutdown(): Unit = {
    schedulers.values().forEach(_.shutdown(true))
  }

  def getScheduler(name: String): Option[Scheduler] = {
    Option(schedulers.get(name))
  }

  def scheduleJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData, notifies: Seq[JobNotify], creator: String): Future[IndexDocResponse] = {
    val schedulerOpt = getScheduler(jobMeta.getScheduler())
    if (schedulerOpt.nonEmpty) {
      val scheduler = schedulerOpt.get
      val job = buildJob(jobMeta, triggerMeta, jobData)
      job.fillCommonFields(creator)
      JobService.index(job).map(res => {
        val (error, jobDetail) = jobMeta.toJobDetail(res.id)
        if (null == error) {
          val triggerOpt = triggerMeta.toTrigger(res.id)
          if (triggerOpt.nonEmpty) {
            try {
              scheduler.scheduleJob(jobDetail, triggerOpt.get)
              IndexDocResponse(res.id)
            } catch {
              case t: Throwable =>
                logger.error(s"job(${jobMeta.group}_${jobMeta.project}_${res.id}) fail to be scheduled")
                JobService.deleteDoc(res.id)
                throw ErrorMessages.error_Throwable(t).toException
            }
          } else {
            scheduler.addJob(jobDetail, true)
            IndexDocResponse(res.id)
          }
        } else {
          throw error.toException
        }
      }).flatMap(indexDocRes => {
        if (null != notifies) {
          notifies.foreach(n => {
            n.fillCommonFields(creator)
            n.jobId = indexDocRes.id
          })
        }
        JobNotifyService.index(notifies)
          .map(_ => indexDocRes)
          .recover {
            case t: Throwable =>
              logger.error(LogUtils.stackTraceToString(t))
              indexDocRes
          }
      })
    } else {
      ErrorMessages.error_NoSchedulerDefined(jobMeta.getScheduler()).toFutureFail
    }
  }

  def pauseJob(job: PauseJob): Future[Boolean] = {
    commonActionValidate(job) { scheduler =>
      Future {
        try {
          scheduler.pauseJob(JobKey.jobKey(job.id, job.getQuartzGroup))
          true
        } catch {
          case t: Throwable => throw ErrorMessages.error_Throwable(t).toException
        }
      }
    }
  }

  def resumeJob(job: ResumeJob): Future[Boolean] = {
    commonActionValidate(job) { scheduler =>
      Future {
        try {
          scheduler.resumeJob(JobKey.jobKey(job.id, job.getQuartzGroup))
          true
        } catch {
          case t: Throwable => throw ErrorMessages.error_Throwable(t).toException
        }
      }
    }
  }

  def deleteJob(job: DeleteJob): Future[Boolean] = {
    commonActionValidate(job) { scheduler =>
      Future {
        try {
          scheduler.deleteJob(JobKey.jobKey(job.id, job.getQuartzGroup))
          true
        } catch {
          case t: Throwable => throw ErrorMessages.error_Throwable(t).toException
        }
      }.flatMap(_ => JobService.deleteDoc(job.id)).map(_ => true)
    }
  }

  def triggerJob(job: TriggerJob): Future[Boolean] = {
    commonActionValidate(job) { scheduler =>
      Future {
        try {
          scheduler.triggerJob(JobKey.jobKey(job.id, job.getQuartzGroup))
          true
        } catch {
          case t: Throwable => throw ErrorMessages.error_Throwable(t).toException
        }
      }
    }
  }

  /** jobName, jobGroup must be same with the old one */
  def updateJob(toUpdate: UpdateJob): Future[String] = {
    val jobMeta = toUpdate.jobMeta
    val triggerMeta = toUpdate.triggerMeta
    val jobData = toUpdate.jobData
    val error = JobUtils.validateJobAndTrigger(jobMeta, triggerMeta, jobData)
    if (null == error) {
      val schedulerOpt = getScheduler(jobMeta.getScheduler())
      val scheduler = schedulerOpt.get
      val (error, jobDetail) = jobMeta.toJobDetail(toUpdate.id)
      if (null == error) {
        val job = buildJob(jobMeta, triggerMeta, jobData)
        JobService.updateJob(toUpdate.id, job).map { res =>
          // replace job
          scheduler.addJob(jobDetail, true)
          // replace trigger
          val triggerKey = TriggerKey.triggerKey(toUpdate.id, JobUtils.generateQuartzGroup(jobMeta.group, jobMeta.project))
          val oldTrigger = scheduler.getTrigger(triggerKey)
          if (null != oldTrigger) {
            val triggerOpt = triggerMeta.toTrigger(toUpdate.id)
            if (triggerOpt.nonEmpty) {
              val newTrigger = triggerOpt.get
              scheduler.rescheduleJob(triggerKey, newTrigger)
            } else {
              scheduler.unscheduleJob(triggerKey)
            }
          } else {
            val triggerOpt = triggerMeta.toTrigger(toUpdate.id)
            if (triggerOpt.nonEmpty) {
              val newTrigger = triggerMeta.triggerType match {
                case TriggerMeta.TYPE_SIMPLE =>
                  triggerOpt.get.asInstanceOf[SimpleTriggerImpl]
                case TriggerMeta.TYPE_CRON =>
                  triggerOpt.get.asInstanceOf[CronTriggerImpl]
              }
              newTrigger.setJobName(toUpdate.id)
              newTrigger.setJobGroup(JobUtils.generateQuartzGroup(jobMeta.group, jobMeta.project))
              scheduler.scheduleJob(newTrigger)
            }
          }
          StringUtils.EMPTY
        }
      } else {
        error.toFutureFail
      }
    } else {
      error.toFutureFail
    }
  }

  def buildJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData): Job = {
    Job(
      summary = jobMeta.summary,
      description = jobMeta.description,
      group = jobMeta.group,
      project = jobMeta.project,
      scheduler = jobMeta.getScheduler(),
      classAlias = jobMeta.getJobAlias(),
      trigger = Seq(JobTrigger(
        group = triggerMeta.group,
        project = triggerMeta.project,
        cron = if (StringUtils.isEmpty(triggerMeta.cron)) StringUtils.EMPTY else triggerMeta.cron,
        triggerType = if (StringUtils.isEmpty(triggerMeta.triggerType)) TriggerMeta.TYPE_MANUAL else triggerMeta.triggerType,
        startNow = if (Option(triggerMeta.startNow).isDefined) triggerMeta.startNow else false,
        startDate = if (Option(triggerMeta.startDate).isDefined && triggerMeta.startDate > 0L) DateUtils.parse(triggerMeta.startDate) else null,
        endDate = if (Option(triggerMeta.endDate).isDefined && triggerMeta.endDate > 0L) DateUtils.parse(triggerMeta.endDate) else null,
        repeatCount = if (Option(triggerMeta.repeatCount).isDefined) triggerMeta.repeatCount else 0,
        interval = if (Option(triggerMeta.interval).isDefined) triggerMeta.interval else 0
      )),
      jobData = jobData,
      env = StringUtils.notEmptyElse(jobMeta.env, StringUtils.EMPTY),
    )
  }

  private def commonActionValidate(action: JobActionValidator)(func: Scheduler => Future[Boolean]): Future[Boolean] = {
    val error = action.validate()
    if (null == error) {
      val scheduler = StringUtils.notEmptyElse(action.scheduler, SchedulerManager.DEFAULT_SCHEDULER)
      val schedulerOpt = getScheduler(scheduler)
      if (schedulerOpt.nonEmpty) {
        func(schedulerOpt.get)
      } else {
        ErrorMessages.error_NoSchedulerDefined(action.scheduler).toFutureFail
      }
    } else {
      error.toFutureFail
    }
  }
}
