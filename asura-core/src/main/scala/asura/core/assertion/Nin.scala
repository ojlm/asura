package asura.core.assertion


import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

object Nin extends CompareOperator with Assertion {

  override val name: String = Assertions.NIN

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    val result = contains(src, target)
    if (result.isSuccessful) {
      FailAssertResult(1)
    } else {
      PassAssertResult(1)
    }
  }
}
