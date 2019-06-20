package asura.core.es.model

case class JobTrigger(
                       group: String,
                       project: String,
                       cron: String = null,
                       triggerType: String = JobTrigger.TYPE_MANUAL,
                       startNow: Boolean = true,
                       startDate: String = null,
                       endDate: String = null,
                       repeatCount: Int = 1,
                       interval: Int = 0
                     )

object JobTrigger {
  val TYPE_MANUAL = "manual"
  val TYPE_SIMPLE = "simple"
  val TYPE_CRON = "cron"
  val TYPE_API = "api"
}
