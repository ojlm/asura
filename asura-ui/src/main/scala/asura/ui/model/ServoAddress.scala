package asura.ui.model

import com.fasterxml.jackson.annotation.JsonIgnore

case class ServoAddress(
                         host: String,
                         port: Int,
                         hostname: String,
                       ) {
  @JsonIgnore
  def toKey = s"${host}:${port}"

}
