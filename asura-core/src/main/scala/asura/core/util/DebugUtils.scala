package asura.core.util

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object DebugUtils {

  def printFuture(future: Future[AnyRef]): Unit = {
    val result = Await.result(future, Duration.Inf)
    println(JacksonSupport.stringify(result))
  }
}
