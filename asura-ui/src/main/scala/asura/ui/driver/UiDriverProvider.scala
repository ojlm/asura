package asura.ui.driver

import asura.ui.model.DriverInfo

import scala.concurrent.Future

trait UiDriverProvider {

  def getDrivers(`type`: String): Future[java.util.Collection[_ <: DriverInfo]]

  def register(`type`: String, info: DriverInfo): Unit

}
