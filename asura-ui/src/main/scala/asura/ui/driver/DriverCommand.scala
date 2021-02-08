package asura.ui.driver

import asura.ui.command.Commands
import asura.ui.model.ServoAddress

case class DriverCommand(
                          name: String,
                          description: String,
                          `type`: String,
                          params: Map[String, Any],
                          servos: Seq[ServoAddress],
                          var meta: CommandMeta,
                          var options: CommandOptions = CommandOptions(),
                        ) {

  def validateServos(): Boolean = {
    Commands.support(`type`) && null != servos && servos.nonEmpty
  }

}
