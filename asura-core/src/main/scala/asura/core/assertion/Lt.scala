package asura.core.assertion

import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

case class Lt() extends Assertion {

  override val name: String = Assertions.LT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Lt.apply(actual, expect))
  }

}

object Lt extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    compareTwo(src, target) { (src, target) =>
      src.compareTo(target) < 0
    }
  }

}
