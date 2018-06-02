package asura.core.cs.operators

import asura.core.cs.asserts._

object Not {

  def apply(ctx: Any, assert: Any): AssertResult = {
    val result = AssertResult()
    assert match {
      case _: Map[_, _] =>
        val subAssert = assert.asInstanceOf[Map[String, Any]]
        val subStatis = Statistic()
        val subResult = Assert(subAssert, ctx.asInstanceOf[Object], subStatis).result
        result.subResult = subResult
        result.pass(subStatis.passed)
        result.fail(subStatis.failed)
        if (subStatis.isSuccessful) {
          result.isSuccessful = false
          result.msg = AssertResult.MSG_FAILED
        } else {
          result.isSuccessful = true
          result.msg = AssertResult.MSG_PASSED
        }
        result
      case _ =>
        FailAssertResult(1, AssertResult.msgIncomparableTargetType(assert))
    }
  }
}
