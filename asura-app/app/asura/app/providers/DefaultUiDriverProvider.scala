package asura.app.providers

import java.util.Collections

import asura.common.util.StringUtils
import asura.ui.driver.{Drivers, UiDriverProvider}
import asura.ui.model.{ChromeDriverInfo, DriverInfo}
import play.api.Configuration

case class DefaultUiDriverProvider(
                                    configuration: Configuration,
                                  ) extends UiDriverProvider {

  val host = configuration.getOptional[String]("asura.ui.proxy.host").getOrElse(StringUtils.EMPTY)
  val port = configuration.getOptional[Int]("asura.ui.proxy.port").getOrElse(0)
  val password = configuration.getOptional[String]("asura.ui.proxy.password").getOrElse(StringUtils.EMPTY)
  val localChrome = ChromeDriverInfo(host, port, password)

  val chromeDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()
  val androidDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()
  val iosDrivers = new java.util.concurrent.ConcurrentHashMap[String, DriverInfo]()

  chromeDrivers.put(localChrome.getKey(), localChrome)

  override def getDrivers(`type`: String): java.util.Collection[DriverInfo] = {
    `type` match {
      case Drivers.CHROME => chromeDrivers.values()
      case Drivers.ANDROID => androidDrivers.values()
      case Drivers.IOS => iosDrivers.values()
      case _ => Collections.emptyList()
    }
  }

  override def register(`type`: String, info: DriverInfo): Unit = {
    if (info != null) info.timestamp = System.currentTimeMillis()
    `type` match {
      case Drivers.CHROME => chromeDrivers.put(info.getKey(), info)
      case Drivers.ANDROID => androidDrivers.put(info.getKey(), info)
      case Drivers.IOS => iosDrivers.put(info.getKey(), info)
      case _ =>
    }
  }

}
