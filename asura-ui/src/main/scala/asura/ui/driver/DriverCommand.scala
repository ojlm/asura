package asura.ui.driver

case class DriverCommand(
                          summary: String,
                          description: String,
                          `type`: String,
                          params: Map[String, Any],
                          var meta: CommandMeta,
                        )
