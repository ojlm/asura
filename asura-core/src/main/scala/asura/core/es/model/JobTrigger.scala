package asura.core.es.model

import asura.common.util.StringUtils

case class JobTrigger(
                       name: String,
                       group: String,
                       description: String = StringUtils.EMPTY,
                       cron: String,
                       triggerType: String = JobTrigger.TYPE_MANUAL,
                       startNow: Boolean = true,
                       startDate: String = StringUtils.EMPTY,
                       endDate: String = StringUtils.EMPTY,
                       repeatCount: Int = 1,
                       interval: Int = 0
                     ) {

}

object JobTrigger {
  val TYPE_MANUAL = "manual"
  val TYPE_SIMPLE = "simple"
  val TYPE_CRON = "cron"
  val TYPE_API = "api"
}
