package asura.web

import akka.actor.ActorRef
import asura.common.actor.ActorEvent
import asura.common.util.LogUtils
import asura.web.BrowserDriverActor.logger
import org.openqa.selenium.chrome.ChromeDriver

class ChromeDriverHandler(req: BrowserRequest, subscriber: ActorRef) extends Runnable {

  override def run(): Unit = {
    try {
      val handlerThread = Thread.currentThread().asInstanceOf[HandlerThread]
      var driver: ChromeDriver = handlerThread.driver.asInstanceOf[ChromeDriver]
      if (null == driver) {
        subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = "Start chrome...")
        handlerThread.driver = new ChromeDriver()
        subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = "chrome start completed")
        driver = handlerThread.driver.asInstanceOf[ChromeDriver]
      }
      val startNano = System.nanoTime()
      driver.get(req.url)
      val logStr = s"request: ${req.url}"
      logger.debug(logStr)
      subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = logStr)
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
