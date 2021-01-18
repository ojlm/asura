package asura.ui.driver

import asura.common.util.DateUtils

case class DriverStatus(
                         var startAt: String = null,
                         var updateAt: String = DateUtils.nowDateTime,
                         var status: String = DriverStatus.STATUS_IDLE,
                         var command: DriverCommand = null,
                       )

object DriverStatus {

  val STATUS_IDLE = "idle"
  val STATUS_RUNNING = "running"

}
