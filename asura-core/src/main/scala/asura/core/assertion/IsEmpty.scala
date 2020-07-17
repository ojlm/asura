package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

case class IsEmpty() extends Assertion {

  override val name: String = Assertions.IS_EMPTY

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(IsEmpty.apply(actual, expect))
  }

}

object IsEmpty {

  def apply(actual: Any, expect: Any): AssertResult = {
    if (null != expect && expect.isInstanceOf[Boolean]) {
      val result = Size(actual, 0)
      if (expect.asInstanceOf[Boolean]) {
        if (result.isSuccessful) PassAssertResult() else FailAssertResult()
      } else {
        if (result.isSuccessful) FailAssertResult() else PassAssertResult()
      }
    } else {
      FailAssertResult(msg = AssertResult.MSG_INCOMPARABLE)
    }
  }

}
