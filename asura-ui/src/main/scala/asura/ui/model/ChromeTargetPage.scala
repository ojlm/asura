package asura.ui.model

import asura.common.util.{HttpUtils, JsonUtils, StringUtils}
import com.fasterxml.jackson.core.`type`.TypeReference

import scala.concurrent.{ExecutionContext, Future}

case class ChromeTargetPage(
                             id: String,
                             `type`: String,
                             title: String,
                             url: String,
                             description: String,
                             devtoolsFrontendUrl: String,
                             webSocketDebuggerUrl: String,
                           )

object ChromeTargetPage {

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

}
