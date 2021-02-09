package asura.ui.util

import asura.common.util.{HttpUtils, JsonUtils, StringUtils}
import asura.ui.model.{ChromeDriverInfo, ChromeTargetPage, ChromeVersion}
import com.fasterxml.jackson.core.`type`.TypeReference

import scala.concurrent.{ExecutionContext, Future}

object ChromeDevTools {

  def getTargetPages(addr: ChromeDriverInfo)(implicit ec: ExecutionContext): Future[Seq[ChromeTargetPage]] = {
    val url = s"http://${addr.host}:${addr.port}/json/list"
    HttpUtils.getAsync(url, classOf[String]).map(body => {
      if (StringUtils.isNotEmpty(body)) {
        JsonUtils.parse(body, new TypeReference[Seq[ChromeTargetPage]]() {})
      } else {
        Nil
      }
    })
  }

  def getVersion(addr: ChromeDriverInfo)(implicit ec: ExecutionContext): Future[ChromeVersion] = {
    val url = s"http://${addr.host}:${addr.port}/json/version"
    HttpUtils.getAsync(url, classOf[ChromeVersion])
  }

}
