package asura.core.sql

import java.util

import asura.common.util.StringUtils
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.JobReportData.JobReportStepItemMetrics
import asura.core.runtime.{AbstractResult, RuntimeContext}
import asura.core.sql.SqlReportModel.{SqlRequestReportModel, SqlResponseReportModel}

import scala.concurrent.Future

case class SqlResult(
                      var docId: String,
                      var assert: Map[String, Any],
                      var context: java.util.Map[Any, Any],
                      var request: SqlRequestReportModel,
                      var response: SqlResponseReportModel,
                      var metrics: JobReportStepItemMetrics = null,
                      var statis: Statistic = Statistic(),
                      var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                      var generator: String = StringUtils.EMPTY,
                    ) extends AbstractResult

object SqlResult {

  def exceptionResult(
                       docId: String,
                       rendered: SqlRequestReportModel = null,
                       context: util.Map[Any, Any] = null,
                     ): SqlResult = {
    val result = SqlResult(
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
                request: SqlRequestReportModel,
                response: SqlResponseReportModel,
              ): Future[SqlResult] = {
    val statistic = Statistic()
    AssertionContext.eval(assert, context.rawContext, statistic).map(assertResult => {
      SqlResult(
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
