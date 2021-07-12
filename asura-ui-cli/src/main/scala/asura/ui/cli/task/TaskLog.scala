package asura.ui.cli.task

import asura.common.util.HostUtils

case class TaskLog(
                    command: String,
                    `type`: String,
                    params: Object,
                    hostname: String = HostUtils.hostname,
                    timestamp: Long = System.currentTimeMillis(),
                  )

object TaskLog {

  val CommandKarate = "karate"

  object LogType {
    val FEATURE = "feature"
    val BACKGROUND = "background"
    val SCENARIO = "scenario"
    val SUITE = "suite"
    val STEP = "step"
  }

  def karate(`type`: String, msg: String): TaskLog = {
    TaskLog(CommandKarate, `type`, msg)
  }

}
