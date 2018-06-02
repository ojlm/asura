package asura.core.job

import java.util.Date

import asura.common.util.StringUtils
import org.quartz.{CronScheduleBuilder, SimpleScheduleBuilder, Trigger, TriggerBuilder}

case class TriggerMeta(
                        name: String,
                        group: String,
                        desc: String = StringUtils.EMPTY,
                        cron: String,
                        triggerType: String = TriggerMeta.TYPE_MANUAL,
                        startNow: Boolean = true,
                        startDate: Long = 0L,
                        endDate: Long = 0L,
                        repeatCount: Int = 0,
                        interval: Int = 0
                      ) {

  def toTrigger(): Option[Trigger] = {
    triggerType match {
      case TriggerMeta.TYPE_MANUAL =>
        None
      case TriggerMeta.TYPE_SIMPLE =>
        val scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
        if (Option(interval).isDefined && interval >= 0) {
          scheduleBuilder.withIntervalInSeconds(interval)
        }
        if (Option(repeatCount).isDefined && repeatCount >= 0) {
          scheduleBuilder.withRepeatCount(repeatCount)
        } else {
          scheduleBuilder.repeatForever()
        }
        val triggerBuilder = TriggerBuilder.newTrigger().withIdentity(name, group).withDescription(desc)
        if (!startNow && Option(startDate).isDefined && startDate > 0) {
          triggerBuilder.startAt(new Date(startDate))
        } else {
          triggerBuilder.startNow()
        }
        if (Option(endDate).isDefined && endDate > 0) {
          triggerBuilder.endAt(new Date(endDate))
        }
        Option(triggerBuilder.withSchedule(scheduleBuilder).build())
      case TriggerMeta.TYPE_CRON =>
        Option(TriggerBuilder.newTrigger()
          .withIdentity(name, group)
          // .withDescription(desc) // saved in es
          .withSchedule(CronScheduleBuilder.cronSchedule(cron))
          .build()
        )
      case TriggerMeta.TYPE_API =>
        // TODO
        None
    }
  }
}

object TriggerMeta {

  val TYPE_MANUAL = "manual"
  val TYPE_SIMPLE = "simple"
  val TYPE_CRON = "cron"
  val TYPE_API = "api"
}
