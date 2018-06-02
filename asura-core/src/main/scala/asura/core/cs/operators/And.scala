package asura.core.cs.operators

import asura.core.cs.asserts._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

object And {

  def apply(ctx: Any, asserts: Any): AssertResult = {
    val result = AssertResult(
      isSuccessful = true,
      msg = AssertResult.MSG_PASSED
    )
    val subResults = ArrayBuffer[mutable.Map[String, Any]]()
    result.subResult = subResults
    asserts match {
      case _: Seq[_] =>
        breakable {
          asserts.asInstanceOf[Seq[Map[String, Any]]].foreach(assert => {
            val subStatis = Statistic()
            subResults += Assert(assert, ctx.asInstanceOf[Object], subStatis).result
            result.pass(subStatis.passed)
            result.fail(subStatis.failed)
            if (!subStatis.isSuccessful) {
              result.isSuccessful = false
              result.msg = AssertResult.MSG_FAILED
              break()
            }
          })
        }
        result
      case _ =>
        FailAssertResult(1, AssertResult.msgIncomparableTargetType(asserts))
    }
  }
}
