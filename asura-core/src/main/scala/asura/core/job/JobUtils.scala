package asura.core.job

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.JobData

object JobUtils {

  def validateJobAndTrigger(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData): ErrorMessages.Val = {
    if (StringUtils.isEmpty(jobMeta.name)) {
      ErrorMessages.error_EmptyJobName
    } else if (StringUtils.isEmpty(jobMeta.group) || StringUtils.isEmpty(triggerMeta.group)) {
      ErrorMessages.error_EmptyJobName
    } else if (StringUtils.isEmpty(jobMeta.classAlias)) {
      ErrorMessages.error_EmptyJobType
    } else if (SchedulerManager.getScheduler(jobMeta.scheduler).isEmpty) {
      ErrorMessages.error_NoSchedulerDefined(jobMeta.scheduler)
    } else {
      // check job data
      val job = JobCenter.classAliasJobMap.get(jobMeta.classAlias)
      if (job.nonEmpty) {
        val (ret, msg) = job.get.checkJobData(jobData)
        if (ret) {
          null
        } else {
          ErrorMessages.error_JobValidate(msg)
        }
      } else {
        ErrorMessages.error_NoJobDefined(jobMeta.classAlias)
      }
    }
  }

  def getJobStdLogPath(jobGroup: String, jobName: String, jobKey: String): String = {
    s"${JobCenter.jobWorkDir}/$jobGroup/$jobName/$jobKey/${JobCenter.jobStdLogFileName}"
  }

  @inline
  def generateQuartzGroup(group: String, project: String) = s"${group}_${project}"
}
