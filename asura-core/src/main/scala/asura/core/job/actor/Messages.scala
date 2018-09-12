package asura.core.job.actor

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.{JobData, JobReport}
import asura.core.job.{JobMeta, TriggerMeta}

// notification messages

case class JobAdded(scheduler: String, jobGroup: String, jobName: String)

case class JobDeleted(scheduler: String, jobGroup: String, jobName: String)

case class JobResumed(scheduler: String, jobGroup: String, jobName: String)

case class JobPaused(scheduler: String, jobGroup: String, jobName: String)

case class JobRunning(scheduler: String, jobGroup: String, jobName: String)

case class JobFinished(scheduler: String, jobGroup: String, jobName: String, report: JobReport)

case class JobScheduled(scheduler: String, jobGroup: String, jobName: String)

case class JobUnscheduled(scheduler: String, triggerGroup: String, triggerName: String)


// action messages

trait JobActionValidator {

  val scheduler: String
  /** doc: group_project */
  val group: String
  /** doc: id */
  val name: String

  def validate(): ErrorMessages.Val = {
    if (StringUtils.isEmpty(scheduler)) {
      ErrorMessages.error_EmptyScheduler
    } else if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(name)) {
      ErrorMessages.error_EmptyJobName
    } else {
      null
    }
  }
}

case class TriggerJob(scheduler: String, group: String, name: String) extends JobActionValidator

case class ResumeJob(scheduler: String, group: String, name: String) extends JobActionValidator

case class PauseJob(scheduler: String, group: String, name: String) extends JobActionValidator

case class DeleteJob(scheduler: String, group: String, name: String, id: String) extends JobActionValidator {
  override def validate(): (Boolean, String) = {
    if (StringUtils.isEmpty(id)) {
      (false, "Empty id")
    } else {
      super.validate()
    }
  }
}

case class NewJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData, creator: String)

case class UpdateJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData)
