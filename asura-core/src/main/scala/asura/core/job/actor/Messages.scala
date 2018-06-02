package asura.core.job.actor

import asura.common.model.BoolErrorRes
import asura.common.util.StringUtils
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
  val group: String
  val name: String

  def validate(): BoolErrorRes = {
    if (StringUtils.isEmpty(scheduler)) {
      (false, "Empty scheduler")
    } else if (StringUtils.isEmpty(group)) {
      (false, "Empty group")
    } else if (StringUtils.isEmpty(name)) {
      (false, "Empty name")
    } else {
      (true, null)
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
