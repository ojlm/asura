package asura.ui.driver

case class CommandMeta(
                        group: String,
                        project: String,
                        taskId: String,
                        creator: String,
                        var reportId: String = null,
                        var day: String = null,
                        var startAt: Long = 0L,
                        var endAt: Long = 0L,
                        var hostname: String = null,
                        var pid: String = null,
                      )
