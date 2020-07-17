package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, PassAssertResult}

import scala.concurrent.Future

case class Eq() extends Assertion {

  override val name: String = Assertions.EQ

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Eq.apply(actual, expect))
  }
}

object Eq extends CompareOperator {

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
