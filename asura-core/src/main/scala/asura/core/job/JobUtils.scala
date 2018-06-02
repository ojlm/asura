package asura.core.job

import asura.common.model.ApiMsg
import asura.common.util.StringUtils
import asura.core.es.model.JobData

object JobUtils {

  def validateJobAndTrigger(jobMeta: JobMeta, triggerMeta: TriggerMeta, jobData: JobData): (Boolean, String) = {
    if (StringUtils.isEmpty(jobMeta.name) || StringUtils.isEmpty(triggerMeta.name)) {
      (false, "任务名称为空")
    } else if (StringUtils.isEmpty(jobMeta.group) || StringUtils.isEmpty(triggerMeta.group)) {
      (false, "任务组名为空")
    } else if (StringUtils.isEmpty(jobMeta.desc) || StringUtils.isEmpty(triggerMeta.desc)) {
      (false, "任务描述为空")
    } else if (StringUtils.isEmpty(jobMeta.classAlias)) {
      (false, "任务类型为空")
    } else if (SchedulerManager.getScheduler(jobMeta.scheduler).isEmpty) {
      (false, "调度器不存在")
    } else {
      // check job data
      val job = JobCenter.classAliasJobMap.get(jobMeta.classAlias)
      if (job.nonEmpty) {
        val (ret, msg) = job.get.checkJobData(jobData)
        if (ret) {
          (true, ApiMsg.SUCCESS)
        } else {
          (false, msg)
        }
      } else {
        (false, "不支持的任务类型")
      }
    }
  }

  def getJobStdLogPath(jobGroup: String, jobName: String, jobKey: String): String = {
    s"${JobCenter.jobWorkDir}/$jobGroup/$jobName/$jobKey/${JobCenter.jobStdLogFileName}"
  }
}
