package asura.ui.model

import asura.common.util.StringUtils
import net.minidev.json.annotate.JsonIgnore

trait DriverInfo {

  val host: String
  val port: Int
  var timestamp: Long = 0L

  @JsonIgnore
  def getKey(): String = {
    s"$host:$port"
  }

  @JsonIgnore
  def isValid(): Boolean = {
    StringUtils.isNotEmpty(host) && port > 0 && port < 65536
  }

}
