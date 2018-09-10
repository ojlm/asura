package asura.core.cs.assertion

import java.util

import asura.core.cs.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

trait CompareOperator {

  type SourceCompareToTarget = (Comparable[Any], Any) => Boolean

  def compareTwo(src: Any, target: Any)(f: SourceCompareToTarget): AssertResult = {
    if (null != src && target != null) {
      if (src.getClass == target.getClass) {
        (src, target) match {
          case (_: Comparable[_], _: Comparable[_]) =>
            val srcComparable = src.asInstanceOf[Comparable[Any]]
            if (f(srcComparable, target)) {
              PassAssertResult(1)
            } else {
              FailAssertResult(1)
            }
          case _ =>
            FailAssertResult(1, AssertResult.msgIncomparableSourceType(src))
        }
      } else {
        FailAssertResult(1, AssertResult.msgNotSameType(src, target))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }

  def contains(src: Any, target: Any): AssertResult = {
    if (null != src && target != null) {
      src match {
        case srcCollection: util.Collection[_] =>
          javaCollectionContains(srcCollection, target)
        case srcSeq: Seq[_] =>
          seqContains(srcSeq, target)
        case srcString: String =>
          stringContains(srcString, target)
        case _ =>
          FailAssertResult(1, AssertResult.msgIncomparableSourceType(src))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }

  @inline
  private def javaCollectionContains(src: util.Collection[_], target: Any): AssertResult = {
    if (src.contains(target)) {
      PassAssertResult(1)
    } else {
      FailAssertResult(1)
    }
  }

  @inline
  private def seqContains(src: Seq[_], target: Any): AssertResult = {
    if (src.contains(target)) {
      PassAssertResult(1)
    } else {
      FailAssertResult(1)
    }
  }

  @inline
  private def stringContains(src: String, target: Any): AssertResult = {
    if (target.isInstanceOf[String]) {
      val targetString = target.asInstanceOf[String]
      if (src.contains(targetString)) {
        PassAssertResult(1)
      } else {
        FailAssertResult(1)
      }
    } else {
      FailAssertResult(1, AssertResult.msgIncomparableSourceType(target))
    }
  }
}
