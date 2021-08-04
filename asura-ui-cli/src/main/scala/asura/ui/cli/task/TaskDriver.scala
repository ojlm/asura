package asura.ui.cli.task

import asura.ui.model.ChromeTargetPage

case class TaskDriver(
                       host: String,
                       port: Integer,
                       driver: Integer,
                       `type`: String,
                       var targets: Seq[ChromeTargetPage] = null,
                     ) {

  private val _hash = s"$host$port$driver${`type`}".hashCode

  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[TaskDriver]) {
      val target = obj.asInstanceOf[TaskDriver]
      host == target.host && port == target.port && driver == target.driver && `type` == target.`type`
    } else {
      false
    }
  }

  override def hashCode(): Int = _hash
}
