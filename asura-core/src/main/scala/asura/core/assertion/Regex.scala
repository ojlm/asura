package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

object Regex extends Assertion {

  override val name: String = Assertions.REGEX

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    if (null != src && target != null) {
      if (src.isInstanceOf[String] && target.isInstanceOf[String]) {
        val first = target.asInstanceOf[String].r.findFirstIn(src.asInstanceOf[String])
        if (first.nonEmpty) {
          PassAssertResult(1)
        } else {
          FailAssertResult(1)
        }
      } else {
        FailAssertResult(1, AssertResult.msgNotSameType(src, target))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }
}
