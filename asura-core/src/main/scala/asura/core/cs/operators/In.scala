package asura.core.cs.operators

import asura.core.cs.asserts.AssertResult

object In extends CompareOperator {

  def apply(src: Any, target: Any): AssertResult = {
    contains(src, target)
  }
}
