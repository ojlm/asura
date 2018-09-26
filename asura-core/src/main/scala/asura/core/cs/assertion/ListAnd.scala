package asura.core.cs.assertion

import asura.core.cs.assertion.engine.AssertResult

import scala.concurrent.Future

object ListAnd extends Assertion {

  override val name: String = Assertions.LIST_AND

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    apply(actual, expect)
  }

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    And(actual, expect)
  }
}
