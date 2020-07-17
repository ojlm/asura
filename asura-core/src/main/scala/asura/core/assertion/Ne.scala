package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

case class Ne() extends Assertion {

  override val name: String = Assertions.NE

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Ne.apply(actual, expect))
  }

}

object Ne extends CompareOperator {

  def apply(actual: Any, expect: Any): AssertResult = {
    if (null == actual && null == expect) {
      FailAssertResult(1)
    } else if (null == actual || null == expect) {
      PassAssertResult(1)
    } else {
      compareTwo(actual, expect) { (src, target) =>
        src.compareTo(target) != 0
      }
    }
  }

}
