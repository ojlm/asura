package asura.core.assertion

import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.assertion.engine.{AssertResult, AssertionContext, FailAssertResult, Statistic}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object And extends Assertion {

  override val name: String = Assertions.AND

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    apply(actual, expect)
  }

  def apply(actual: Any, expect: Any): Future[AssertResult] = {
    val result = AssertResult(
      isSuccessful = true,
      msg = AssertResult.MSG_PASSED
    )
    expect match {
      case assertions: Seq[_] =>
        if (assertions.nonEmpty) {
          val assertionResults = assertions.map(assertion => {
            val subStatis = Statistic()
            val assertionMap = assertion.asInstanceOf[Map[String, Any]]
            val contextMap = actual.asInstanceOf[Object]
            AssertionContext.eval(assertionMap, contextMap, subStatis).map((subStatis, _))
          })
          Future.sequence(assertionResults).map(subStatisResults => {
            val subResults = ArrayBuffer[java.util.Map[String, Any]]()
            result.subResult = subResults
            subStatisResults.foreach(subStatisResult => {
              val (subStatis, subResult) = subStatisResult
              subResults += subResult
              result.pass(subStatis.passed)
              result.fail(subStatis.failed)
              if (!subStatis.isSuccessful) {
                result.isSuccessful = false
                result.msg = AssertResult.MSG_FAILED
              }
            })
            result
          })
        } else {
          Future.successful(null)
        }
      case _ =>
        Future.successful(FailAssertResult(1, AssertResult.msgIncomparableTargetType(expect)))
    }
  }
}
