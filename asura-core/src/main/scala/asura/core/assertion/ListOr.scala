package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class ListOr() extends Assertion {

  override val name: String = Assertions.LIST_OR

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    ListOr.apply(actual, expect)
  }

}

object ListOr {

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    Or(actual, expect)
  }

}
