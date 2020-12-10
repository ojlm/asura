package asura.app.providers

import asura.common.util.StringUtils
import asura.ui.driver.{UiDriverAddress, UiDriverProvider}
import play.api.Configuration

case class DefaultUiDriverProvider(
                                    configuration: Configuration,
                                  ) extends UiDriverProvider {

  val host = configuration.getOptional[String]("asura.ui.proxy.host").getOrElse(StringUtils.EMPTY)
  val port = configuration.getOptional[Int]("asura.ui.proxy.port").getOrElse(0)
  val password = configuration.getOptional[String]("asura.ui.proxy.password").getOrElse(StringUtils.EMPTY)

  override def getDrivers(): Seq[UiDriverAddress] = {
    Seq(UiDriverAddress(host, port, password, UiDriverAddress.DRIVER_TYPE_CHROME))
  }

}
