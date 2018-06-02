package asura.web

import java.net.URI

import akka.actor.ActorRef
import asura.common.actor.NotifyActorEvent
import asura.common.util.{LogUtils, StringUtils, XtermUtils}
import asura.web.BrowserDriverActor.logger
import com.gargoylesoftware.htmlunit.{BrowserVersion, WebClient}
import org.openqa.selenium.htmlunit.HtmlUnitDriver

case class HtmlUnitHandler(req: BrowserRequest, subscriber: ActorRef) extends Runnable {

  override def run(): Unit = {
    try {
      val handlerThread = Thread.currentThread().asInstanceOf[HandlerThread]
      var driver: HtmlUnitDriver = handlerThread.driver.asInstanceOf[HtmlUnitDriver]
      if (null == driver) {
        subscriber ! NotifyActorEvent(s"${XtermUtils.greenWrap(handlerThread.name)}:Using HtmlUnit...")
        handlerThread.driver = new HtmlUnitDriverExt(BrowserVersion.BEST_SUPPORTED)
        subscriber ! NotifyActorEvent(s"${XtermUtils.greenWrap(handlerThread.name)}:HtmlUnit is initialized")
        driver = handlerThread.driver.asInstanceOf[HtmlUnitDriver]
      }
      val startNano = System.nanoTime()
      driver.get(new URI(req.url).toASCIIString)
      val logStr = s"${XtermUtils.greenWrap(handlerThread.name)}:${req.url}"
      logger.debug(logStr)
      subscriber ! NotifyActorEvent(logStr)
      val pageSource = driver.getPageSource
      if (null != subscriber) {
        val elapse = (System.nanoTime() - startNano) / 1000000
        subscriber ! BrowserResponse(req.id, true, null, elapse, -1, pageSource)
      }
    } catch {
      case t: InterruptedException =>
        logger.warn(LogUtils.stackTraceToString(t))
        if (null != subscriber) {
          subscriber ! BrowserResponse(req.id, false, t.getMessage, -1, -1, null)
        }
        val handlerThread = Thread.currentThread().asInstanceOf[HandlerThread]
        val driver = handlerThread.driver
        driver.quit()
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        if (null != subscriber) {
          subscriber ! BrowserResponse(req.id, false, t.getMessage, -1, -1, null)
        }
    }
  }
}

class HtmlUnitDriverExt(browserVersion: BrowserVersion) extends HtmlUnitDriver(browserVersion) {
  override def getPageSource: String = {
    val page = lastPage
    if (page == null) {
      return StringUtils.EMPTY
    }
    val response = page.getWebResponse
    return response.getContentAsString
  }

  override def getWebClient: WebClient = {
    super.getWebClient
  }
}
