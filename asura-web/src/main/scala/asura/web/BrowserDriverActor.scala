package asura.web

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import java.util.logging.LogManager

import akka.actor.Props
import asura.common.actor.BaseActor
import com.typesafe.scalalogging.Logger

object BrowserDriverActor {
  val props = Props[BrowserDriverActor]
  val logger = Logger(getClass)
  // Disable HtmlUnit Warning
  LogManager.getLogManager().reset()
  java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE)
}

class BrowserDriverActor extends BaseActor {
  private val executors: ExecutorService = Executors.newFixedThreadPool(2, HandlerThreadFactory())

  override def receive: Receive = {
    case requests: BrowserRequests =>
      requests.requests.foreach(req => executors.submit(HtmlUnitHandler(req, sender())))
    case request: BrowserRequest =>
      executors.submit(JBrowserHandler(request, sender()))
  }

  override def postStop() {
    shutdown()
  }

  def shutdown(): Unit = {
    executors.shutdownNow()
    executors.awaitTermination(0, TimeUnit.MICROSECONDS)
  }
}
