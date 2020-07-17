package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class Gt() extends Assertion {

  override val name: String = Assertions.GT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Gt.apply(actual, expect))
  }
}

object Gt extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) > 0
    }
  }
}
