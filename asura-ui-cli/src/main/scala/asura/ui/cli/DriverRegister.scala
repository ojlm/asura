package asura.ui.cli

import java.util
import java.util.Collections

import asura.common.model.{ApiCode, ApiRes}
import asura.common.util.HttpUtils
import asura.ui.cli.CliSystem.ec
import asura.ui.driver.UiDriverProvider
import asura.ui.model.DriverInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

case class DriverRegister(pushUrl: String) extends UiDriverProvider {

  val logger = Logger(classOf[DriverRegister])

  override def getDrivers(`type`: String): Future[util.Collection[_ <: DriverInfo]] = Future.successful(Collections.emptyList())

  override def register(`type`: String, info: DriverInfo): Unit = {
    val url = s"$pushUrl/api/ui/driver/register/${`type`}"
    HttpUtils.postJson(url, info, classOf[ApiRes]).map(res => {
      if (ApiCode.OK.equals(res.code)) {
        logger.info(s"push to url $url success")
      } else {
        logger.error(s"push to url $url error: ${res.msg}")
      }
    }).recover {
      case t: Throwable => logger.error(s"push to url: $url", t)
    }
  }

}
