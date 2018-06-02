package asura.core.cs.operators

import asura.core.cs.asserts.AssertResult

object Nor {

  def apply(ctx: Any, assert: Any): AssertResult = {
    val result = Or(ctx, assert)
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
  }
}
