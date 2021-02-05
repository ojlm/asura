package asura.app.api.model

import asura.ui.command.Commands
import asura.ui.driver.DriverCommand
import asura.ui.model.ServerAddress
import com.fasterxml.jackson.annotation.JsonIgnore

case class RunUiTask(
                      command: DriverCommand,
                      servers: Seq[ServerAddress]
                    ) {

  @JsonIgnore
  def validate(): Boolean = {
    null != command && Commands.support(command.`type`) && null != servers && servers.nonEmpty
  }

}
