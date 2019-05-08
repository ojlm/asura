package asura.core.http

import akka.http.scaladsl.model.HttpResponse
import asura.common.util.StringUtils
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.runtime.RuntimeContext
import asura.core.es.model.JobReportData.JobReportStepItemMetrics

import scala.concurrent.Future

case class HttpResult(
                       var caseId: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: HttpRequestModel,
                       var response: HttpResponseModel,
                       var metrics: JobReportStepItemMetrics = null,
                       var statis: Statistic = Statistic(),
                       var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                       var generator: String = StringUtils.EMPTY, // generator type
                     )

object HttpResult {

  def failResult(caseId: String): HttpResult = {
    val result = HttpResult(
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
            context: RuntimeContext,
            request: HttpRequestModel,
            caseResponse: HttpResponseModel,
          ): Future[HttpResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map { assertResult =>
      HttpResult(
        caseId = caseId,
        assert = assert,
        context = context.rawContext,
        request = request,
        response = caseResponse,
        statis = statistic,
        result = assertResult
      )
    }
  }
}
