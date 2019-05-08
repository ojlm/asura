package asura.core.http

import akka.http.scaladsl.model.HttpResponse
import asura.common.util.StringUtils
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.runtime.{AbstractResult, RuntimeContext}

import scala.concurrent.Future

case class HttpResult(
                       var docId: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: HttpRequestReportModel,
                       var response: HttpResponseReportModel,
                       var metrics: JobReportStepItemMetrics = null,
                       var statis: Statistic = Statistic(),
                       var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                       var generator: String = StringUtils.EMPTY, // generator type
                     ) extends AbstractResult

object HttpResult {

  def failResult(docId: String): HttpResult = {
    val result = HttpResult(
      docId = docId,
      assert = null,
      context = null,
      request = null,
      response = null
    )
    result.statis.isSuccessful = false
    result
  }

  def eval(
            docId: String,
            response: HttpResponse,
            assert: Map[String, Any],
            context: RuntimeContext,
            request: HttpRequestReportModel,
            caseResponse: HttpResponseReportModel,
          ): Future[HttpResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map { assertResult =>
      HttpResult(
        docId = docId,
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
