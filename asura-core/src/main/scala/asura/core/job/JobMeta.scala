package asura.core.job

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.job.impl.RunCaseJob
import org.quartz.{JobBuilder, JobDetail}

case class JobMeta(
                    var group: String,
                    var project: String,
                    summary: String,
                    comment: String = null,
                    env: String = StringUtils.EMPTY,
                    description: String = StringUtils.EMPTY,
                    scheduler: String = StringUtils.EMPTY, // this field can be null, use method instead
                    classAlias: String // the field can be null, use method instead
                  ) {

  def toJobDetail(docId: String): (ErrorMessage, JobDetail) = {
    val clazz = JobCenter.supportedJobClasses.get(getJobAlias())
    if (clazz.isEmpty) {
      (ErrorMessages.error_NoJobDefined(getJobAlias()), null)
    } else {
      val jobDetail = JobBuilder.newJob(clazz.get)
        .withIdentity(docId, JobUtils.generateQuartzGroup(group, project))
        .storeDurably(true)
        .build()
      (null, jobDetail)
    }
  }

  def getJobAlias(): String = {
    if (StringUtils.isNotEmpty(classAlias)) {
      classAlias
    } else {
      RunCaseJob.meta.classAlias
    }
  }

  def getScheduler(): String = {
    if (StringUtils.isNotEmpty(scheduler)) {
      scheduler
    } else {
      SchedulerManager.DEFAULT_SCHEDULER
    }
  }
}
