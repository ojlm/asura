package asura.ui.ide.model

import asura.common.util.StringUtils
import asura.ui.driver.Drivers
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.TaskRecord.RecordData

case class TaskRecord(
                       var workspace: String,
                       var project: String,
                       var task: String = StringUtils.EMPTY,
                       var `type`: Int = Task.TYPE_DEBUG,
                       var driver: String = Drivers.CHROME,
                       var source: String = TaskRecord.SOURCE_MANUAL,
                       var startAt: Long = 0,
                       var endAt: Long = 0,
                       var elapse: Long = 0,
                       var data: RecordData = null,
                     ) extends AbsDoc {

  def parse(): Unit = {
    if (StringUtils.isEmpty(workspace)) {
      throw IdeErrors.WORKSPACE_NAME_EMPTY
    }
    if (StringUtils.isEmpty(project)) {
      throw IdeErrors.PROJECT_NAME_EMPTY
    }
    if (`type` != Task.TYPE_DEBUG && `type` != Task.TYPE_MONKEY && `type` != Task.TYPE_PLAN) {
      throw IdeErrors.TASK_TYPE_ILLEGAL
    }
    if (driver != Drivers.CHROME && driver != Drivers.ANDROID) {
      throw IdeErrors.TASK_DRIVER_ILLEGAL_MSG(driver)
    }
    if (source != TaskRecord.SOURCE_MANUAL && source != TaskRecord.SOURCE_SCHEDULE) {
      throw IdeErrors.RECORD_SOURCE_ILLEGAL_MSG(source)
    }
  }

}

object TaskRecord {

  val SOURCE_MANUAL = "manual"
  val SOURCE_SCHEDULE = "schedule"

  case class RecordData(
                         addr: Seq[Address]
                       )

}
