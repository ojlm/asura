package asura.ui.ide.model

import asura.common.util.StringUtils
import asura.ui.driver.Drivers
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.Task.TaskData

case class Task(
                 var workspace: String,
                 var project: String,
                 var name: String = StringUtils.EMPTY,
                 var description: String = StringUtils.EMPTY,
                 var `type`: Int = Task.TYPE_DEBUG,
                 var driver: String = Drivers.CHROME,
                 var data: TaskData = null,
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
  }

}

object Task {

  val TYPE_DEBUG = 0
  val TYPE_MONKEY = 1
  val TYPE_PLAN = 2

  case class TaskData(

                     )

}
