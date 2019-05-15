package asura.core.dubbo

import akka.pattern.ask
import akka.util.Timeout
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.dubbo.DubboReportModel.{DubboRequestReportModel, DubboResponseReportModel}
import asura.core.es.model.DubboRequest
import asura.core.es.model.DubboRequest.DubboRequestBody
import asura.core.runtime.{ContextOptions, RuntimeContext, RuntimeMetrics}
import asura.core.{CoreConfig, RunnerActors}
import asura.dubbo.GenericRequest
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object DubboRunner {

  val logger = Logger("DubboRunner")
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
  lazy val dubboInvoker = RunnerActors.dubboInvoker

  def test(docId: String, request: DubboRequest, context: RuntimeContext = RuntimeContext()): Future[DubboResult] = {
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
        .flatMap(genericRequest => {
          metrics.performRequestStart()
          (dubboInvoker ? genericRequest).flatMap(responseObj => {
            context.setCurrentEntity(responseObj.asInstanceOf[Object])
            metrics.evalAssertionBegin()
            DubboResult.evaluate(
              docId,
              request.assert,
              context,
              DubboRequestReportModel(genericRequest),
              DubboResponseReportModel(responseObj.asInstanceOf[Object])
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

  def renderRequest(request: DubboRequestBody, context: RuntimeContext)(implicit metrics: RuntimeMetrics): Future[GenericRequest] = {
    metrics.renderRequestEnd()
    metrics.renderAuthBegin()
    metrics.renderAuthEnd()
    val parameterTypes = if (null != request.parameterTypes && request.parameterTypes.nonEmpty) {
      request.parameterTypes.map(_.`type`).toArray
    } else {
      null
    }
    val args = if (StringUtils.isNotEmpty(request.args)) {
      val renderedText = context.renderBodyAsString(request.args)
      JsonUtils.parse(renderedText, classOf[Array[Object]])
    } else {
      null
    }
    val genericRequest = GenericRequest(
      dubboGroup = request.dubboGroup,
      interface = request.interface,
      method = request.method,
      parameterTypes = parameterTypes,
      args = args,
      address = request.address,
      port = request.port,
      version = request.version
    )
    Future.successful(genericRequest)
  }
}
