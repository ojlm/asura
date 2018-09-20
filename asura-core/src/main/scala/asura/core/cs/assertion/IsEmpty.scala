package asura.core.cs.assertion

import asura.core.cs.assertion.engine.{AssertResult, FailAssertResult}

import scala.concurrent.Future

object IsEmpty extends Assertion {

  override val name: String = Assertions.IS_EMPTY

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(actual: Any, expect: Any): AssertResult = {
    if (null != expect && expect.isInstanceOf[Boolean]) {
      val result = Size(actual, 0)
      if (expect.asInstanceOf[Boolean]) {
        result
      } else {
        FailAssertResult()
      }
    } else {
      FailAssertResult(msg = AssertResult.MSG_INCOMPARABLE)
    }
  }
}
