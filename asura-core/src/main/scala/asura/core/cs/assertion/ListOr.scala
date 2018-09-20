package asura.core.cs.assertion

import asura.core.cs.assertion.engine.AssertResult

import scala.concurrent.Future

object ListOr extends Assertion {

  override val name: String = Assertions.LIST_OR

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    apply(actual, expect)
  }

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    Or(actual, expect)
  }
}
