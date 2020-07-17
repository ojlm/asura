package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class ListAnd() extends Assertion {

  override val name: String = Assertions.LIST_AND

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    ListAnd.apply(actual, expect)
  }
}

object ListAnd {

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    And(actual, expect)
  }

}
