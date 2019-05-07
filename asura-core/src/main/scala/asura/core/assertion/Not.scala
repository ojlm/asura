package asura.core.assertion

import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.assertion.engine.{AssertResult, AssertionContext, FailAssertResult, Statistic}

import scala.concurrent.Future

object Not extends Assertion {

  override val name: String = Assertions.NOT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    apply(actual, expect)
  }

  def apply(ctx: Any, assert: Any): Future[AssertResult] = {
    val result = AssertResult()
    assert match {
      case _: Map[_, _] =>
        val subAssert = assert.asInstanceOf[Map[String, Any]]
        val subStatis = Statistic()
        AssertionContext.eval(subAssert, ctx.asInstanceOf[Object], subStatis).map(subResult => {
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
        })
      case _ =>
        Future.successful(FailAssertResult(1, AssertResult.msgIncomparableTargetType(assert)))
    }
  }
}
