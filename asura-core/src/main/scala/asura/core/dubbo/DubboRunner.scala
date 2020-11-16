package asura.core.dubbo

import akka.pattern.ask
import akka.util.Timeout
import asura.common.exceptions.{RequestFailException, WithDataException}
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.dubbo.RenderedDubboModel.{RenderedDubboRequest, RenderedDubboResponse}
import asura.core.es.model.DubboRequest
import asura.core.es.model.DubboRequest.{DubboRequestBody, LoadBalanceAlgorithms}
import asura.core.runtime.{ContextOptions, RuntimeContext, RuntimeMetrics}
import asura.core.{CoreConfig, RunnerActors}
import asura.dubbo.GenericRequest
import asura.dubbo.actor.GenericServiceInvokerActor.GetProvidersMessage
import asura.dubbo.model.DubboProvider
import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.RandomUtils

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
      options.stepEnv = request.env
    } else {
      options = ContextOptions(stepEnv = request.env)
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
            context.setCurrentMetrics(metrics)
            DubboResult.evaluate(
              docId,
              request.assert,
              context,
              RenderedDubboRequest(genericRequest),
              RenderedDubboResponse(responseObj.asInstanceOf[Object])
            )
          }).recover {
            case t: Throwable => throw WithDataException(t, genericRequest)
          }
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
      val renderedText = context.renderTemplateAsString(request.args)
      JsonUtils.parse(renderedText, classOf[Array[Object]])
    } else {
      null
    }
    if (request.enableLb) {
      getTargetAddressAndPort(request).map(tuple => {
        GenericRequest(
          dubboGroup = request.dubboGroup,
          interface = request.interface,
          method = request.method,
          parameterTypes = parameterTypes,
          args = args,
          address = tuple._1,
          port = tuple._2,
          version = request.version
        )
      })
    } else {
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

  def getTargetAddressAndPort(request: DubboRequestBody): Future[(String, Int)] = {
    val msg = GetProvidersMessage(request.zkConnectString, request.path, request.interface)
    (dubboInvoker ? msg).map(res => {
      val providers = res.asInstanceOf[Seq[DubboProvider]]
      if (providers.nonEmpty) {
        request.lbAlgorithm match {
          case LoadBalanceAlgorithms.RANDOM =>
            val provider = providers(RandomUtils.nextInt(0, providers.length))
            (provider.address, provider.port)
          case _ =>
            // default use `random`
            val provider = providers(RandomUtils.nextInt(0, providers.length))
            (provider.address, provider.port)
        }
      } else {
        throw RequestFailException("There is not available dubbo provider")
      }
    })
  }
}
