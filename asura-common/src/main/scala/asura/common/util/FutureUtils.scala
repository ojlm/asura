package asura.common.util

import asura.common.exceptions.{IllegalRequestException, RequestFailException}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object FutureUtils {

  @inline
  def illegalArgs(msg: String) = Future.failed(new IllegalArgumentException(msg))

  @inline
  def illegalRequest(msg: String, data: Any) = Future.failed(IllegalRequestException(msg, data))

  @inline
  def requestFail(msg: String) = Future.failed(RequestFailException(msg))

  implicit class RichFuture[T](future: Future[T]) {
    def await(implicit duration: Duration = 60 seconds): T = Await.result(future, duration)
  }

}
