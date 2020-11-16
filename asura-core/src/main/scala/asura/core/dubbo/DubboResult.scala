package asura.core.dubbo

import java.util

import asura.common.util.StringUtils
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.dubbo.RenderedDubboModel.{RenderedDubboRequest, RenderedDubboResponse}
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.runtime.{AbstractResult, RuntimeContext}

import scala.concurrent.Future

case class DubboResult(
                        var docId: String,
                        var assert: Map[String, Any],
                        var context: java.util.Map[Any, Any],
                        var request: RenderedDubboRequest,
                        var response: RenderedDubboResponse,
                        var metrics: JobReportStepItemMetrics = null,
                        var statis: Statistic = Statistic(),
                        var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                        var generator: String = StringUtils.EMPTY,
                      ) extends AbstractResult

object DubboResult {

  def exceptionResult(
                       docId: String,
                       rendered: RenderedDubboRequest = null,
                       context: util.Map[Any, Any] = null,
                     ): DubboResult = {
    val result = DubboResult(
      docId = docId,
      assert = null,
      context = context,
      request = rendered,
      response = null
    )
    result.statis.isSuccessful = false
    result
  }

  def evaluate(
                docId: String,
                assert: Map[String, Any],
                context: RuntimeContext,
                request: RenderedDubboRequest,
                response: RenderedDubboResponse,
              ): Future[DubboResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map(assertResult => {
      DubboResult(
        docId = docId,
        assert = assert,
        context = context.rawContext,
        request = request,
        response = response,
        statis = statistic,
        result = assertResult
      )
    })
  }
}
