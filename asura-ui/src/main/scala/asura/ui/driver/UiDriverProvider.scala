package asura.ui.driver

import asura.ui.model.DriverInfo

trait UiDriverProvider {

  def getDrivers(`type`: String): java.util.Collection[DriverInfo]

  def register(`type`: String, info: DriverInfo): Unit

}
