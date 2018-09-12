package asura.core.job

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import org.quartz.{JobBuilder, JobDetail}

case class JobMeta(
                    group: String,
                    project: String,
                    name: String,
                    desc: String = StringUtils.EMPTY,
                    scheduler: String = StringUtils.EMPTY,
                    classAlias: String
                  ) {

  def toJobDetail(docId: String): (ErrorMessages.Val, JobDetail) = {
    val clazz = JobCenter.supportedJobClasses.get(classAlias)
    if (clazz.isEmpty) {
      (ErrorMessages.error_NoJobDefined(classAlias), null)
    } else {
      val jobDetail = JobBuilder.newJob(clazz.get)
        .withIdentity(docId, JobUtils.generateQuartzGroup(group, project))
        .storeDurably(true)
        .build()
      (null, jobDetail)
    }
  }
}
