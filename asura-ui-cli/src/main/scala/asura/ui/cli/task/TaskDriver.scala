package asura.ui.cli.task

import asura.ui.model.ChromeTargetPage

case class TaskDriver(
                       host: String,
                       port: Integer,
                       driver: Integer,
                       var targets: Seq[ChromeTargetPage] = null,
                     ) {
  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[TaskDriver]) {
      val target = obj.asInstanceOf[TaskDriver]
      host == target.host && port == target.port && driver == target.driver
    } else {
      false
    }
  }
}
