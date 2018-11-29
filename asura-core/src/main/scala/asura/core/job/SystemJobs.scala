package asura.core.job

import java.util.Date

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.job.impl.ClearJobReportDataIndicesJob
import org.quartz.Trigger.TriggerState
import org.quartz._
import org.quartz.impl.triggers.CronTriggerImpl

import scala.concurrent.Future

object SystemJobs {

  def putOrUpdateClearJobReportIndicesJob(job: ClearJobReportIndicesJobModel): Future[Date] = {
    if (job.day > 0 && StringUtils.isNotEmpty(job.cron)) {
      Future {
        val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
        if (schedulerOpt.nonEmpty) {
          val scheduler = schedulerOpt.get
          val triggerKey = TriggerKey.triggerKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(job.cron))
            .build()
            .asInstanceOf[CronTriggerImpl]
          val jobKey = JobKey.jobKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val jobDetail = JobBuilder.newJob(classOf[ClearJobReportDataIndicesJob])
            .withIdentity(jobKey)
            .storeDurably(true)
            .build()
          val dataMap = jobDetail.getJobDataMap
          dataMap.put(ClearJobReportDataIndicesJob.KEY_CRON, job.cron)
          dataMap.put(ClearJobReportDataIndicesJob.KEY_DAY, job.day.toString)
          scheduler.addJob(jobDetail, true)
          val oldTrigger = scheduler.getTrigger(triggerKey)
          if (null != oldTrigger) {
            scheduler.rescheduleJob(triggerKey, trigger)
          } else {
            trigger.setJobName(jobKey.getName)
            trigger.setJobGroup(jobKey.getGroup)
            scheduler.scheduleJob(trigger)
          }
        } else {
          throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
        }
      }
    } else {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    }
  }

  def getClearReportIndicesJob(): Future[ClearJobReportIndicesJobModel] = {
    Future {
      val response = ClearJobReportIndicesJobModel()
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        val detail = scheduler.getJobDetail(JobKey.jobKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER))
        if (null != detail) {
          val dataMap = detail.getJobDataMap
          response.day = dataMap.getInt(ClearJobReportDataIndicesJob.KEY_DAY)
          val triggerKey = TriggerKey.triggerKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val state = scheduler.getTriggerState(triggerKey)
          response.state = state.name()
          response.cron = dataMap.getString(ClearJobReportDataIndicesJob.KEY_CRON)
          if (StringUtils.isNotEmpty(response.cron) && TriggerState.NORMAL == state) {
            val expression = new CronExpression(response.cron)
            response.next = expression.getNextValidTimeAfter(new Date())
          }
        }
      } else {
        throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
      }
      response
    }
  }

  def pauseClearReportIndicesJob(): Future[Boolean] = {
    Future {
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        scheduler.pauseJob(JobKey.jobKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER))
        true
      } else {
        throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
      }
    }
  }

  def resumeClearReportIndicesJob(): Future[Boolean] = {
    Future {
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        scheduler.resumeJob(JobKey.jobKey(ClearJobReportDataIndicesJob.NAME, SchedulerManager.SYSTEM_SCHEDULER))
        true
      } else {
        throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
      }
    }
  }

  case class ClearJobReportIndicesJobModel(
                                            var cron: String = "0 0 3 * * ?",
                                            var state: String = JobStates.UNCREATED,
                                            var day: Int = 20,
                                            var next: Date = null
                                          )

  case class SyncDomainAndApiJobModel(
                                       var cron: String = "0 0 3 * * ?",
                                       var state: String = JobStates.UNCREATED,
                                       var day: Int = 20,
                                       var domainCount: Int = 1000,
                                       var apiCount: Int = 2000,
                                       var next: Date = null
                                     )

}
