package asura.web

import java.util.concurrent.ThreadFactory

case class HandlerThreadFactory() extends ThreadFactory {
  var count = 0

  override def newThread(r: Runnable): Thread = {
    count = count + 1
    HandlerThread(r, s"browser-driver-$count")
  }
}
