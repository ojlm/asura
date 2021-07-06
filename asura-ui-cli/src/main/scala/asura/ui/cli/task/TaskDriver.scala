package asura.ui.cli.task

import asura.ui.model.ChromeTargetPage

case class TaskDriver(
                       host: String,
                       port: Integer,
                       driver: Integer,
                       var targets: Seq[ChromeTargetPage] = null,
                     )
