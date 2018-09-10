package asura.core.cs.assertion

import asura.core.cs.assertion.engine.{AssertResult, PassAssertResult}

import scala.concurrent.Future

object Eq extends CompareOperator with Assertion {

  override val name: String = Assertions.EQ

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(actual: Any, expect: Any): AssertResult = {
    if (null == actual && expect == null) {
      PassAssertResult(1)
    } else {
      compareTwo(actual, expect) { (src, target) =>
        src.compareTo(target) == 0
      }
    }
  }
}
