package asura.ui.driver

case class DriverCommand(
                          `type`: String,
                          params: Map[String, Any],
                          var creator: String,
                        )
