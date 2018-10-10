package asura.core.job.actor

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.{JobData, JobNotify, JobReport}
import asura.core.job.{JobMeta, JobUtils, TriggerMeta}

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
  val group: String
  val project: String
  val id: String

  def validate(): ErrorMessages.ErrorMessage = {
    if (StringUtils.isEmpty(scheduler)) {
      ErrorMessages.error_EmptyScheduler
    } else if (StringUtils.isEmpty(group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId
    } else {
      null
    }
  }

  def getQuartzGroup = JobUtils.generateQuartzGroup(group, project)
}

case class TriggerJob(scheduler: String, group: String, project: String, id: String) extends JobActionValidator

case class ResumeJob(scheduler: String, group: String, project: String, id: String) extends JobActionValidator

case class PauseJob(scheduler: String, group: String, project: String, id: String) extends JobActionValidator

case class DeleteJob(scheduler: String, group: String, project: String, id: String) extends JobActionValidator

case class NewJob(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData, notifies: Seq[JobNotify], creator: String)

case class UpdateJob(id: String, jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData)
