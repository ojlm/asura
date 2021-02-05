package asura.ui.model

import asura.common.util.StringUtils
import asura.ui.driver.DriverStatus
import net.minidev.json.annotate.JsonIgnore

trait DriverInfo {

  val host: String
  val port: Int
  var timestamp: Long = 0L
  var hostname: String = null
  var screenCapture: String = null // base64 image
  var status: DriverStatus = null

  @JsonIgnore
  def getKey(): String = {
    s"$host:$port"
  }

  @JsonIgnore
  def valid(): Boolean = {
    StringUtils.isNotEmpty(host) && port > 0 && port < 65536
  }

}
