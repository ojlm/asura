package asura.core.cs.operators

import asura.core.cs.asserts.{AssertResult, FailAssertResult, PassAssertResult}

object Regex {

  def apply(src: Any, target: Any): AssertResult = {
    if (null != src && target != null) {
      if (src.isInstanceOf[String] && target.isInstanceOf[String]) {
        val first = target.asInstanceOf[String].r.findFirstIn(src.asInstanceOf[String])
        if (first.nonEmpty) {
          PassAssertResult(1)
        } else {
          FailAssertResult(1)
        }
      } else {
        FailAssertResult(1, AssertResult.msgNotSameType(src, target))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }
}
