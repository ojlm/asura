package asura.core.assertion

import asura.core.assertion.engine.AssertResult
import asura.core.concurrent.ExecutionContextManager.cachedExecutor

import scala.concurrent.Future

case class Nor() extends Assertion {

  override val name: String = Assertions.NOR

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Nor.apply(actual, expect)
  }

}

object Nor {

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    Or(actual, expect).map(result => {
      if (result.isSuccessful && AssertResult.MSG_PASSED.equals(result.msg)) {
        result.isSuccessful = false
        result.msg = AssertResult.MSG_FAILED
        result
      } else if (!result.isSuccessful && AssertResult.MSG_FAILED.equals(result.msg)) {
        result.isSuccessful = true
        result.msg = AssertResult.MSG_PASSED
        result
      } else {
        result
      }
    })
  }
}
