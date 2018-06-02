package asura.core.job

import asura.core.job.actor._
import com.typesafe.scalalogging.Logger
import org.quartz._

object NamedSchedulerListener {
  val logger = Logger(classOf[NamedSchedulerListener])

  def apply(name: String): NamedSchedulerListener = new NamedSchedulerListener(name)
}

class NamedSchedulerListener(val name: String) extends SchedulerListener {

  import NamedSchedulerListener._

  override def schedulerStarting(): Unit = {
    logger.info(s"[$name] schedulerStarting")
  }

  override def schedulerStarted(): Unit = {
    logger.info(s"[$name] schedulerStarted")
  }

  override def schedulerShutdown(): Unit = {
    logger.info(s"[$name] schedulerShutdown")
  }

  override def schedulerShuttingdown(): Unit = {
    logger.info(s"[$name] schedulerShuttingdown")
  }

  override def schedulerError(msg: String, cause: SchedulerException): Unit = {
    logger.info(s"[$name] schedulerError, msg: $msg, cause: ${cause.getMessage}")
  }

  override def schedulerInStandbyMode(): Unit = {
    logger.info(s"[$name] schedulerInStandbyMode")
  }

  override def schedulingDataCleared(): Unit = {
    logger.info(s"[$name] schedulingDataCleared")
  }

  // job events

  override def jobAdded(jobDetail: JobDetail): Unit = {
    val jobKey = jobDetail.getKey
    val jobName = jobKey.getName
    val jobGroup = jobKey.getGroup
    logger.debug(s"[$name] job($jobGroup, $jobName) added")
    SchedulerActor.statusMonitor ! JobAdded(name, jobGroup, jobName)
  }

  override def jobPaused(jobKey: JobKey): Unit = {
    val jobName = jobKey.getName
    val jobGroup = jobKey.getGroup
    logger.debug(s"[$name] job($jobGroup, $jobName) paused")
    SchedulerActor.statusMonitor ! JobPaused(name, jobGroup, jobName)
  }

  override def jobResumed(jobKey: JobKey): Unit = {
    val jobName = jobKey.getName
    val jobGroup = jobKey.getGroup
    logger.debug(s"[$name] job($jobGroup, $jobName resumed")
    SchedulerActor.statusMonitor ! JobResumed(name, jobGroup, jobName)
  }

  override def jobDeleted(jobKey: JobKey): Unit = {
    val jobName = jobKey.getName
    val jobGroup = jobKey.getGroup
    logger.debug(s"[$name] job($jobGroup, $jobName) deleted")
    SchedulerActor.statusMonitor ! JobDeleted(name, jobGroup, jobName)
  }

  override def jobScheduled(trigger: Trigger): Unit = {
    val jobKey = trigger.getJobKey
    val jobName = jobKey.getName
    val jobGroup = jobKey.getGroup
    logger.debug(s"[$name] job($jobGroup,$jobName) scheduled")
    SchedulerActor.statusMonitor ! JobScheduled(name, jobGroup, jobName)
  }

  override def jobUnscheduled(triggerKey: TriggerKey): Unit = {
    logger.debug(s"[$name] trigger(${triggerKey.getName}, ${triggerKey.getGroup}) unscheduled")
    val triggerName = triggerKey.getName
    val triggerGroup = triggerKey.getGroup
    SchedulerActor.statusMonitor ! JobUnscheduled(name, triggerGroup, triggerName)
  }

  override def jobsPaused(jobGroup: String): Unit = {
    logger.debug(s"[$name] jobsPaused: $jobGroup")
  }

  override def jobsResumed(jobGroup: String): Unit = {
    logger.debug(s"[$name] jobsResumed: $jobGroup")
  }

  // Trigger events

  override def triggerPaused(triggerKey: TriggerKey): Unit = {
    logger.debug(s"[$name] trigger(${triggerKey.getName}, ${triggerKey.getGroup}) paused")
  }

  override def triggerResumed(triggerKey: TriggerKey): Unit = {
    logger.debug(s"[$name] trigger(${triggerKey.getName}, ${triggerKey.getGroup}) resumed")
  }

  override def triggerFinalized(trigger: Trigger): Unit = {
    val triggerKey = trigger.getKey
    logger.debug(s"[$name] trigger(${triggerKey.getName}, ${triggerKey.getGroup}) finalized")
  }

  override def triggersPaused(triggerGroup: String): Unit = {
    logger.debug(s"[$name] triggersPaused: $triggerGroup")
  }

  override def triggersResumed(triggerGroup: String): Unit = {
    logger.debug(s"[$name] triggersResumed: $triggerGroup")
  }
}

