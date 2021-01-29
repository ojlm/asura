package asura.app.providers

import java.util.Collections

import asura.ui.driver.{Drivers, UiDriverProvider}
import asura.ui.model.DriverInfo
import play.api.Configuration

import scala.concurrent.Future

case class DefaultUiDriverProvider(
                                    configuration: Configuration,
                                  ) extends UiDriverProvider {

  override def getDrivers(`type`: String): Future[java.util.Collection[_ <: DriverInfo]] = {
    val drivers = `type` match {
      case Drivers.CHROME => DefaultUiDriverProvider.chromeDrivers.values()
      case Drivers.ANDROID => DefaultUiDriverProvider.androidDrivers.values()
      case Drivers.IOS => DefaultUiDriverProvider.iosDrivers.values()
      case _ => Collections.emptyList()
    }
    Future.successful(drivers)
  }

  override def register(`type`: String, info: DriverInfo): Unit = {
    if (info != null) info.timestamp = System.currentTimeMillis()
    `type` match {
      case Drivers.CHROME => DefaultUiDriverProvider.chromeDrivers.put(info.getKey(), info)
      case Drivers.ANDROID => DefaultUiDriverProvider.androidDrivers.put(info.getKey(), info)
      case Drivers.IOS => DefaultUiDriverProvider.iosDrivers.put(info.getKey(), info)
      case _ =>
    }
  }

}

object DefaultUiDriverProvider {
  val chromeDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()
  val androidDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()
  val iosDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()
}
