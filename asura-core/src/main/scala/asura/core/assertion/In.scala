package asura.core.assertion


import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

object In extends CompareOperator with Assertion {

  override val name: String = Assertions.IN

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    contains(src, target)
  }
}
