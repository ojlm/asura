package asura.core.cs.operators

import asura.core.cs.asserts.{AssertResult, FailAssertResult, PassAssertResult}

object Nin extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    val result = contains(src, target)
    if (result.isSuccessful) {
      FailAssertResult(1)
    } else {
      PassAssertResult(1)
    }
  }
}
