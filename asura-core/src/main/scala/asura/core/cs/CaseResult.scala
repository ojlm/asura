package asura.core.cs

import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.cs.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.Case

import scala.concurrent.Future

case class CaseResult(
                       var id: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: CaseRequest,
                       var statis: Statistic = Statistic(),
                       var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP
                     )

object CaseResult {

  def failResult(id: String, cs: Case): CaseResult = {
    val result = CaseResult(id, cs.assert, null, null)
    result.statis.isSuccessful = false
    result
  }

  def eval(
            id: String,
            assert: Map[String, Any],
            context: java.util.Map[Any, Any],
            request: CaseRequest
          ): Future[CaseResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context, statistic).map { assertResult =>
      CaseResult(id, assert, context, request, statistic, assertResult)
    }
  }
}
