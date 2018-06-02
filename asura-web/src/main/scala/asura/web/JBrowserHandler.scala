package asura.web

import akka.actor.ActorRef
import asura.common.actor.ActorEvent
import asura.common.util.LogUtils
import asura.web.BrowserDriverActor.logger
import com.machinepublishers.jbrowserdriver.JBrowserDriver

case class JBrowserHandler(req: BrowserRequest, subscriber: ActorRef) extends Runnable {

  override def run(): Unit = {
    try {
      val handlerThread = Thread.currentThread().asInstanceOf[HandlerThread]
      var driver: JBrowserDriver = handlerThread.driver.asInstanceOf[JBrowserDriver]
      if (null == driver) {
        subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = "start browser...")
        handlerThread.driver = new JBrowserDriver(BrowserSettings.chromeSettings)
        subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = "browser start completed")
        driver = handlerThread.driver.asInstanceOf[JBrowserDriver]
      }
      val startNano = System.nanoTime()
      driver.get(req.url)
      var status = driver.getStatusCode
      val logStr = s"request: ${req.url}, status: ${status}"
      logger.debug(logStr)
      subscriber ! ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = logStr)
      val pageSource = driver.getPageSource
      if (499 == status) {
        // https://github.com/MachinePublishers/jBrowserDriver/issues/280
        status = 200
      }
      if (null != subscriber) {
        val elapse = (System.nanoTime() - startNano) / 1000000
        subscriber ! BrowserResponse(req.id, true, null, elapse, status, pageSource)
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
