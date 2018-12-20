package asura.core.job

import java.util.Date

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.job.impl.{ClearJobReportDataIndicesJob, SyncOnlineDomainAndRestApiJob}
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

  def putOrUpdateSyncDomainApiJob(job: SyncDomainAndApiJobModel): Future[Date] = {
    if (job.day > 0 && StringUtils.isNotEmpty(job.cron) && job.domainCount > 0 && job.apiCount > 0) {
      Future {
        val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
        if (schedulerOpt.nonEmpty) {
          val scheduler = schedulerOpt.get
          val triggerKey = TriggerKey.triggerKey(SyncOnlineDomainAndRestApiJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(job.cron))
            .build()
            .asInstanceOf[CronTriggerImpl]
          val jobKey = JobKey.jobKey(SyncOnlineDomainAndRestApiJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val jobDetail = JobBuilder.newJob(classOf[SyncOnlineDomainAndRestApiJob])
            .withIdentity(jobKey)
            .storeDurably(true)
            .build()
          val dataMap = jobDetail.getJobDataMap
          dataMap.put(SyncOnlineDomainAndRestApiJob.KEY_CRON, job.cron)
          dataMap.put(SyncOnlineDomainAndRestApiJob.KEY_DAY, job.day.toString)
          dataMap.put(SyncOnlineDomainAndRestApiJob.KEY_DOMAIN_COUNT, job.domainCount.toString)
          dataMap.put(SyncOnlineDomainAndRestApiJob.KEY_API_COUNT, job.apiCount.toString)
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

  def getSyncDomainAndApiJob(): Future[SyncDomainAndApiJobModel] = {
    Future {
      val response = SyncDomainAndApiJobModel()
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        val detail = scheduler.getJobDetail(JobKey.jobKey(SyncOnlineDomainAndRestApiJob.NAME, SchedulerManager.SYSTEM_SCHEDULER))
        if (null != detail) {
          val dataMap = detail.getJobDataMap
          response.day = dataMap.getInt(SyncOnlineDomainAndRestApiJob.KEY_DAY)
          response.domainCount = dataMap.getInt(SyncOnlineDomainAndRestApiJob.KEY_DOMAIN_COUNT)
          response.apiCount = dataMap.getInt(SyncOnlineDomainAndRestApiJob.KEY_API_COUNT)
          val triggerKey = TriggerKey.triggerKey(SyncOnlineDomainAndRestApiJob.NAME, SchedulerManager.SYSTEM_SCHEDULER)
          val state = scheduler.getTriggerState(triggerKey)
          response.state = state.name()
          response.cron = dataMap.getString(SyncOnlineDomainAndRestApiJob.KEY_CRON)
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

  def pauseSystemJob(name: String): Future[Boolean] = {
    Future {
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        scheduler.pauseJob(JobKey.jobKey(name, SchedulerManager.SYSTEM_SCHEDULER))
        true
      } else {
        throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
      }
    }
  }

  def resumeSystemJob(name: String): Future[Boolean] = {
    Future {
      val schedulerOpt = SchedulerManager.getScheduler(SchedulerManager.SYSTEM_SCHEDULER)
      if (schedulerOpt.nonEmpty) {
        val scheduler = schedulerOpt.get
        scheduler.resumeJob(JobKey.jobKey(name, SchedulerManager.SYSTEM_SCHEDULER))
        true
      } else {
        throw ErrorMessages.error_NoSchedulerDefined(SchedulerManager.SYSTEM_SCHEDULER).toException
      }
    }
  }

  case class ClearJobReportIndicesJobModel(
                                            var cron: String = "0 0 3 * * ?",
                                            var state: String = JobStates.UNCREATED,
                                            var day: Int = ClearJobReportDataIndicesJob.DEFAULT_DAY,
                                            var next: Date = null
                                          )

  case class SyncDomainAndApiJobModel(
                                       var cron: String = "0 0 3 * * ?",
                                       var state: String = JobStates.UNCREATED,
                                       var day: Int = SyncOnlineDomainAndRestApiJob.DEFAULT_DAY,
                                       var domainCount: Int = SyncOnlineDomainAndRestApiJob.DEFAULT_DOMAIN_COUNT,
                                       var apiCount: Int = SyncOnlineDomainAndRestApiJob.DEFAULT_API_COUNT,
                                       var next: Date = null
                                     )

}
