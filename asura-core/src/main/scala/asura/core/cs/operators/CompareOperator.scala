package asura.core.cs.operators

import java.util

import asura.core.cs.asserts.{AssertResult, FailAssertResult, PassAssertResult}

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
        case _: util.Collection[_] =>
          val srcCollection = src.asInstanceOf[util.Collection[Any]]
          if (srcCollection.contains(target)) {
            PassAssertResult(1)
          } else {
            FailAssertResult(1)
          }
        case _: Seq[_] =>
          val srcSeq = src.asInstanceOf[Seq[_]]
          if (srcSeq.contains(target)) {
            PassAssertResult(1)
          } else {
            FailAssertResult(1)
          }
        case _: String =>
          if (target.isInstanceOf[String]) {
            val srcString = src.asInstanceOf[String]
            val targetString = target.asInstanceOf[String]
            if (srcString.contains(targetString)) {
              PassAssertResult(1)
            } else {
              FailAssertResult(1)
            }
          } else {
            FailAssertResult(1, AssertResult.msgIncomparableSourceType(target))
          }
        case _ =>
          FailAssertResult(1, AssertResult.msgIncomparableSourceType(src))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }
}
