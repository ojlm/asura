package asura.core.cs.assertion

import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.cs.assertion.engine.AssertResult

import scala.concurrent.Future

object Nor extends Assertion {

  override val name: String = Assertions.NOR

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    apply(actual, expect)
  }

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
