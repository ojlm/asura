package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class In() extends Assertion {

  override val name: String = Assertions.IN

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(In.apply(actual, expect))
  }

}

object In extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    contains(src, target)
  }

}
