package asura.ui.cli.task

import asura.common.util.HostUtils

case class TaskLog(
                    command: String,
                    `type`: String,
                    params: Object,
                    hostname: String = HostUtils.hostname,
                    timestamp: Long = System.currentTimeMillis(),
                  )
