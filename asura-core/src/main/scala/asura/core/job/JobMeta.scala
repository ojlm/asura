package asura.core.job

import asura.common.model.{ApiMsg, BoolErrorTypeRes}
import asura.common.util.StringUtils
import org.quartz.{JobBuilder, JobDetail}

case class JobMeta(
                    name: String,
                    group: String = StringUtils.EMPTY,
                    desc: String = StringUtils.EMPTY,
                    scheduler: String = StringUtils.EMPTY,
                    classAlias: String
                  ) {

  def toJobDetail(): BoolErrorTypeRes[JobDetail] = {
    val clazz = JobCenter.supportedJobClasses.get(classAlias)
    if (clazz.isEmpty) {
      (false, ErrorMsg.ERROR_INVALID_JOB_CLASS, null)
    } else {
      val jobDetail = JobBuilder.newJob(clazz.get)
        .withIdentity(name, group)
        //.withDescription(desc) // saved in es
        .storeDurably(true)
        .build()
      (true, ApiMsg.SUCCESS, jobDetail)
    }
  }
}

object JobMeta {
}
