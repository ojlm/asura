package asura.ui.model

import asura.ui.command.CommandRunner
import asura.ui.model.DriverInitResponse.ServoInitResponseItem
import com.fasterxml.jackson.annotation.JsonIgnore

case class DriverInitResponse(
                               servos: Seq[ServoInitResponseItem],
                             ) {

  def isAllOk(): Boolean = {
    servos.forall(item => item.ok)
  }

}

object DriverInitResponse {

  case class ServoInitResponseItem(
                                    servo: ServoAddress,
                                    ok: Boolean,
                                    errorMsg: String,
                                  ) {
    @JsonIgnore
    var runner: CommandRunner = null

  }

}
