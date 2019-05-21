package asura.core.sql

import akka.pattern.ask
import akka.util.Timeout
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.SqlRequest
import asura.core.es.model.SqlRequest.SqlRequestBody
import asura.core.runtime.{ContextOptions, RuntimeContext, RuntimeMetrics}
import asura.core.sql.SqlReportModel.{SqlRequestReportModel, SqlResponseReportModel}
import asura.core.{CoreConfig, RunnerActors}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object SqlRunner {

  val logger = Logger("SqlRunner")
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
  lazy val sqlInvoker = RunnerActors.sqlInvoker

  def test(docId: String, request: SqlRequest, context: RuntimeContext = RuntimeContext()): Future[SqlResult] = {
    implicit val metrics = RuntimeMetrics()
    metrics.start()
    context.eraseCurrentData()
    var options = context.options
    if (null != options) {
      options.caseEnv = request.env
    } else {
      options = ContextOptions(caseEnv = request.env)
      context.options = options
    }
    metrics.renderRequestStart()
    context.evaluateOptions().flatMap(_ => {
      renderRequest(request.request, context)
        .flatMap(tuple => {
          metrics.performRequestStart()
          (sqlInvoker ? tuple._1).flatMap(responseObj => {
            context.setCurrentEntity(responseObj.asInstanceOf[Object])
            metrics.evalAssertionBegin()
            SqlResult.evaluate(
              docId,
              request.assert,
              context,
              tuple._2,
              SqlResponseReportModel(responseObj.asInstanceOf[Object])
            )
          })
        })
        .map(result => {
          metrics.evalAssertionEnd()
          metrics.theEnd()
          result.metrics = metrics.toReportStepItemMetrics()
          result
        })
    })
  }

  def renderRequest(request: SqlRequestBody, context: RuntimeContext)
                   (implicit metrics: RuntimeMetrics): Future[(SqlRequestBody, SqlRequestReportModel)] = {
    val host = request.host
    val port = request.port
    val database = request.database
    val sql = context.renderBodyAsString(request.sql)
    metrics.renderRequestEnd()
    metrics.renderAuthBegin()
    metrics.renderAuthEnd()
    val renderedRequest = request.copyFrom(host, port, database, sql)
    val reportModel = SqlRequestReportModel(
      host = request.host,
      port = request.port,
      username = request.username,
      database = request.database,
      table = request.table,
      sql = sql
    )
    Future.successful((renderedRequest, reportModel))
  }

}
