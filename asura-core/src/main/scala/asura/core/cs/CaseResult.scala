package asura.core.cs

import akka.http.scaladsl.model.HttpResponse
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.cs.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.JobReportData.CaseReportItemMetrics

import scala.concurrent.Future

case class CaseResult(
                       var caseId: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: CaseRequest,
                       var response: CaseResponse,
                       var metrics: CaseReportItemMetrics = null,
                       var statis: Statistic = Statistic(),
                       var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP
                     )

object CaseResult {

  def failResult(caseId: String): CaseResult = {
    val result = CaseResult(
      caseId = caseId,
      assert = null,
      context = null,
      request = null,
      response = null
    )
    result.statis.isSuccessful = false
    result
  }

  def eval(
            caseId: String,
            response: HttpResponse,
            assert: Map[String, Any],
            context: CaseContext,
            request: CaseRequest,
          ): Future[CaseResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map { assertResult =>
      CaseResult(
        caseId = caseId,
        assert = assert,
        context = context.rawContext,
        request = request,
        response = context.getCaseResponse(response.status.reason()),
        statis = statistic,
        result = assertResult
      )
    }
  }
}
