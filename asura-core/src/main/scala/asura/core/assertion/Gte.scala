package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class Gte() extends Assertion {

  override val name: String = Assertions.GTE

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Gte.apply(actual, expect))
  }

}

object Gte extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) >= 0
    }
  }

}
