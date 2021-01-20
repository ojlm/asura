package asura.ui.driver

case class DriverCommand(
                          name: String,
                          description: String,
                          `type`: String,
                          params: Map[String, Any],
                          var meta: CommandMeta,
                          var options: CommandOptions = CommandOptions(),
                        )
