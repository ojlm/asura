package asura.core.job

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.JobData

object JobUtils {

  def validateJobAndTrigger(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData): ErrorMessages.ErrorMessage = {
    if (null == jobMeta || null == triggerMeta || null == jobData) {
      ErrorMessages.error_EmptyRequestBody
    } else if ((null == jobData.cs && null == jobData.scenario) ||
      (null != jobData.cs && jobData.cs.isEmpty && null != jobData.scenario && jobData.scenario.isEmpty)) {
      ErrorMessages.error_EmptyJobCaseScenarioCount
    } else if (StringUtils.isEmpty(jobMeta.summary)) {
      ErrorMessages.error_EmptyJobName
    } else if (StringUtils.isEmpty(jobMeta.group) || StringUtils.isEmpty(triggerMeta.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(jobMeta.project) || StringUtils.isEmpty(triggerMeta.project)) {
      ErrorMessages.error_EmptyProject
    } else {
      // check job data
      val job = JobCenter.classAliasJobMap.get(jobMeta.getJobAlias())
      if (job.nonEmpty) {
        val (ret, msg) = job.get.checkJobData(jobData)
        if (ret) {
          null
        } else {
          ErrorMessages.error_JobValidate(msg)
        }
      } else {
        ErrorMessages.error_NoJobDefined(jobMeta.getJobAlias())
      }
    }
  }

  def getJobStdLogPath(jobGroup: String, jobName: String, jobKey: String): String = {
    s"${JobCenter.jobWorkDir}/$jobGroup/$jobName/$jobKey/${JobCenter.jobStdLogFileName}"
  }

  @inline
  def generateQuartzGroup(group: String, project: String) = s"${group}_${project}"
}
