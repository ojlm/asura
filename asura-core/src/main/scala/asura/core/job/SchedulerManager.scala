package asura.core.job

import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{DateUtils, FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.es.model.{Job, JobData, JobTrigger}
import asura.core.es.service.JobService
import asura.core.job.actor._
import com.typesafe.scalalogging.Logger
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.triggers.{CronTriggerImpl, SimpleTriggerImpl}
import org.quartz.{JobKey, Scheduler, TriggerKey}

import scala.concurrent.Future

object SchedulerManager {

  val logger = Logger("SchedulerManager")
  val schedulers = new ConcurrentHashMap[String, Scheduler]()

  def init(props: Properties*): Unit = {
    props.foreach(prop => {
      val scheduler = new StdSchedulerFactory(prop).getScheduler
      val name = scheduler.getSchedulerName
      schedulers.put(name, scheduler)
      scheduler.getListenerManager.addSchedulerListener(NamedSchedulerListener(name))
      scheduler.start()
    })
  }

  def shutdown(): Unit = {
    schedulers.values().forEach(_.shutdown(true))
  }

  def getScheduler(name: String): Option[Scheduler] = {
    Option(schedulers.get(name))
  }

  def scheduleJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData, creator: String): Future[String] = {
    val schedulerOpt = getScheduler(jobMeta.scheduler)
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
              val date = scheduler.scheduleJob(jobDetail, triggerOpt.get)
              date.toString
            } catch {
              case t: Throwable =>
                logger.error(s"job(${jobMeta.group}_${jobMeta.project}_${res.id}) fail to be scheduled")
                JobService.deleteDoc(res.id)
                throw ErrorMessages.error_Throwable(t).toException
            }
          } else {
            scheduler.addJob(jobDetail, true)
            StringUtils.EMPTY
          }
        } else {
          throw error.toException
        }
      })
    } else {
      ErrorMessages.error_NoSchedulerDefined(jobMeta.scheduler).toFutureFail
    }
  }

  def pauseJob(job: PauseJob): BoolErrorRes = {
    val error = job.validate()
    if (!isOk) {
      (false, errMsg)
    } else {
      val schedulerOpt = getScheduler(job.scheduler)
      if (schedulerOpt.nonEmpty) {
        try {
          schedulerOpt.get.pauseJob(JobKey.jobKey(job.name, job.group))
          (true, ApiMsg.SUCCESS)
        } catch {
          case t: Throwable => (false, t.getMessage)
        }
      } else {
        (false, ErrorMsg.ERROR_INVALID_SCHEDULER_TYPE)
      }
    }
  }

  def resumeJob(job: ResumeJob): BoolErrorRes = {
    val (isOk, errMsg) = job.validate()
    if (!isOk) {
      (false, errMsg)
    } else {
      val schedulerOpt = getScheduler(job.scheduler)
      if (schedulerOpt.nonEmpty) {
        try {
          schedulerOpt.get.resumeJob(JobKey.jobKey(job.name, job.group))
          (true, ApiMsg.SUCCESS)
        } catch {
          case t: Throwable => (false, t.getMessage)
        }
      } else {
        (false, ErrorMsg.ERROR_INVALID_SCHEDULER_TYPE)
      }
    }
  }

  def deleteJob(job: DeleteJob): BoolErrorRes = {
    val (isOk, errMsg) = job.validate()
    if (!isOk) {
      (false, errMsg)
    } else {
      val schedulerOpt = getScheduler(job.scheduler)
      if (schedulerOpt.nonEmpty) {
        try {
          schedulerOpt.get.deleteJob(JobKey.jobKey(job.name, job.group))
          JobService.deleteDoc(job.id)
          (true, ApiMsg.SUCCESS)
        } catch {
          case t: Throwable => (false, t.getMessage)
        }
      } else {
        (false, ErrorMsg.ERROR_INVALID_SCHEDULER_TYPE)
      }
    }
  }

  def triggerJob(job: TriggerJob): BoolErrorRes = {
    val (isOk, errMsg) = job.validate()
    if (!isOk) {
      (false, errMsg)
    } else {
      val schedulerOpt = getScheduler(job.scheduler)
      if (schedulerOpt.nonEmpty) {
        try {
          schedulerOpt.get.triggerJob(JobKey.jobKey(job.name, job.group))
          (true, ApiMsg.SUCCESS)
        } catch {
          case t: Throwable => (false, t.getMessage)
        }
      } else {
        (false, ErrorMsg.ERROR_INVALID_SCHEDULER_TYPE)
      }
    }
  }

  /** jobName, jobGroup must be same with the old one */
  def updateJob(jobToUpdate: UpdateJob): Future[String] = {
    val jobMeta = jobToUpdate.jobMeta
    val triggerMeta = jobToUpdate.triggerMeta
    val jobData = jobToUpdate.jobData
    val error = JobUtils.validateJobAndTrigger(jobMeta, triggerMeta, jobData)
    if (null == error) {
      val schedulerOpt = getScheduler(jobMeta.scheduler)
      val scheduler = schedulerOpt.get
      val (isOk, errMsg, jobDetail) = jobMeta.toJobDetail()
      if (isOk) {
        val job = buildJob(jobMeta, triggerMeta, jobData)
        JobService.updateJob(job).map { res =>
          // replace job
          scheduler.addJob(jobDetail, true)
          // replace trigger
          val triggerKey = TriggerKey.triggerKey(triggerMeta.name, triggerMeta.group)
          val oldTrigger = scheduler.getTrigger(triggerKey)
          if (null != oldTrigger) {
            val triggerOpt = triggerMeta.toTrigger()
            if (triggerOpt.nonEmpty) {
              val newTrigger = triggerOpt.get
              scheduler.rescheduleJob(triggerKey, newTrigger)
            } else {
              scheduler.unscheduleJob(triggerKey)
            }
          } else {
            val triggerOpt = triggerMeta.toTrigger()
            if (triggerOpt.nonEmpty) {
              val newTrigger = triggerMeta.triggerType match {
                case TriggerMeta.TYPE_SIMPLE =>
                  triggerOpt.get.asInstanceOf[SimpleTriggerImpl]
                case TriggerMeta.TYPE_CRON =>
                  triggerOpt.get.asInstanceOf[CronTriggerImpl]
              }
              newTrigger.setJobName(jobMeta.name)
              newTrigger.setJobGroup(jobMeta.group)
              scheduler.scheduleJob(newTrigger)
            }
          }
          StringUtils.EMPTY
        }
      } else {
        FutureUtils.illegalArgs(errMsg)
      }
    } else {
      error.toFutureFail
    }
  }

  def buildJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData): Job = {
    Job(
      summary = jobMeta.name,
      description = jobMeta.desc,
      name = jobMeta.name,
      group = jobMeta.group,
      project = jobMeta.project,
      scheduler = jobMeta.scheduler,
      classAlias = jobMeta.classAlias,
      trigger = Seq(JobTrigger(
        group = triggerMeta.group,
        project = triggerMeta.project,
        cron = if (StringUtils.isEmpty(triggerMeta.cron)) StringUtils.EMPTY else triggerMeta.cron,
        triggerType = if (StringUtils.isEmpty(triggerMeta.triggerType)) StringUtils.EMPTY else triggerMeta.triggerType,
        startNow = if (Option(triggerMeta.startNow).isDefined) triggerMeta.startNow else false,
        startDate = if (Option(triggerMeta.startDate).isDefined && triggerMeta.startDate > 0L) DateUtils.parse(triggerMeta.startDate) else null,
        endDate = if (Option(triggerMeta.endDate).isDefined && triggerMeta.endDate > 0L) DateUtils.parse(triggerMeta.endDate) else null,
        repeatCount = if (Option(triggerMeta.repeatCount).isDefined) triggerMeta.repeatCount else 0,
        interval = if (Option(triggerMeta.interval).isDefined) triggerMeta.interval else 0
      )),
      jobData = jobData
    )
  }
}

