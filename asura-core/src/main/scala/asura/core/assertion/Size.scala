package asura.core.assertion

import java.util

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

case class Size() extends Assertion {

  override val name: String = Assertions.SIZE

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Size.apply(actual, expect))
  }

}

object Size {

  def apply(src: Any, target: Any): AssertResult = {
    if (null != src && target != null) {
      if (target.isInstanceOf[Int]) {
        val size = target.asInstanceOf[Int]
        src match {
          case _: util.Collection[_] =>
            val srcCollection = src.asInstanceOf[util.Collection[_]]
            if (srcCollection.size() == size) {
              PassAssertResult(1)
            } else {
              FailAssertResult(1)
            }
          case _: Seq[_] =>
            val srcSeq = src.asInstanceOf[Seq[_]]
            if (srcSeq.size == size) {
              PassAssertResult(1)
            } else {
              FailAssertResult(1)
            }
          case _: String =>
            val srcStr = src.asInstanceOf[String]
            if (srcStr.length == size) {
              PassAssertResult(1)
            } else {
              FailAssertResult(1)
            }
          case _ =>
            FailAssertResult(1, AssertResult.msgIncomparableSourceType(src))
        }
      } else {
        FailAssertResult(1, AssertResult.msgIncomparableTargetType(target))
      }
    } else {
      FailAssertResult(1, s"Null value, src: $src, target: $target")
    }
  }

}
