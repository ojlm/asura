package asura.core.ci.actor

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{LogUtils, StringUtils}
import asura.core.ci.actor.ReadinessCheckActor.{DoCheck, DoDelay, DoRetry, ReadinessRequest}
import asura.core.dubbo.DubboRunner
import asura.core.es.model.CiTrigger.ReadinessCheck
import asura.core.es.model.{DubboRequest, HttpStepRequest, ScenarioStep, SqlRequest}
import asura.core.es.service.{DubboRequestService, HttpRequestService, SqlRequestService}
import asura.core.http.HttpRunner
import asura.core.job.JobExecDesc
import asura.core.runtime.AbstractResult
import asura.core.sql.SqlRunner

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

// only send back a tuple of `(boolean, string)`, stop self when job is done.
class ReadinessCheckActor(readiness: ReadinessCheck) extends BaseActor {

  implicit val ec = context.dispatcher
  var asker: ActorRef = null
  var doRequest: () => Future[AbstractResult] = null
  var retryCount = 0

  override def receive: Receive = {
    case DoCheck =>
      asker = sender()
      getTargetDoc() pipeTo self
    case ReadinessRequest(request, errMsg) =>
      if (null == request) {
        answerAndStopSelf(false, errMsg)
      } else {
        request match {
          case req: DubboRequest =>
            this.doRequest = () => DubboRunner.test(StringUtils.EMPTY, req)
            self ! DoDelay
          case req: HttpStepRequest =>
            this.doRequest = () => HttpRunner.test(StringUtils.EMPTY, req)
            self ! DoDelay
          case req: SqlRequest =>
            this.doRequest = () => SqlRunner.test(StringUtils.EMPTY, req)
            self ! DoDelay
          case _ => answerAndStopSelf(false, "Unknown request type")
        }
      }
    case DoDelay =>
      if (readiness.delay > 0) {
        context.system.scheduler
          .scheduleOnce(readiness.delay seconds) {
            self ! DoRetry
          }
      } else {
        self ! DoRetry
      }
    case DoRetry =>
      // if retries is 1, then it should try 2 times
      if (needRetry()) {
        retryCount = retryCount + 1
        getAndEvalResult()
      } else {
        answerAndStopSelf(false, ReadinessCheckActor.ERROR_RETRY_EXCEED)
      }
  }

  private def getAndEvalResult(): Unit = {
    if (null != this.doRequest) {
      val futureResult = this.doRequest()
      val promiseResult = Promise[AbstractResult]()
      val timeoutCancellable = context.system.scheduler.scheduleOnce(readiness.timeout seconds) {
        promiseResult.tryFailure(new RuntimeException("Connection timeout"))
      }
      futureResult.onComplete {
        case scala.util.Success(result) =>
          timeoutCancellable.cancel()
          promiseResult.trySuccess(result)
        case scala.util.Failure(t) =>
          timeoutCancellable.cancel()
          promiseResult.tryFailure(t)
      }
      promiseResult.future.onComplete {
        case scala.util.Success(result) =>
          if (result.statis.isSuccessful) {
            answerAndStopSelf(true, JobExecDesc.STATUS_SUCCESS)
          } else {
            doRetry(JobExecDesc.STATUS_FAIL)
          }
        case scala.util.Failure(t) => doRetry(t.getMessage)
      }
    } else {
      answerAndStopSelf(false, "Not implemented function")
    }
  }

  private def doRetry(errMsg: String): Unit = {
    if (needRetry()) {
      if (readiness.interval > 0) {
        context.system.scheduler
          .scheduleOnce(readiness.interval seconds) {
            self ! DoRetry
          }
      } else {
        self ! DoRetry
      }
    } else {
      answerAndStopSelf(false, errMsg)
    }
  }

  @inline
  private def needRetry(): Boolean = retryCount <= readiness.retries

  private def answerAndStopSelf(isOk: Boolean, errMsg: String): Unit = {
    asker ! (isOk, errMsg)
    context stop self
  }

  private def getTargetDoc(): Future[ReadinessRequest] = {
    readiness.targetType match {
      case ScenarioStep.TYPE_HTTP =>
        HttpRequestService.getRequestById(readiness.targetId)
          .recover { case t: Throwable => ReadinessRequest(null, LogUtils.stackTraceToString(t)) }
          .map(req => ReadinessRequest(req))
      case ScenarioStep.TYPE_DUBBO =>
        DubboRequestService.getRequestById(readiness.targetId)
          .recover { case t: Throwable => ReadinessRequest(null, LogUtils.stackTraceToString(t)) }
          .map(req => ReadinessRequest(req))
      case ScenarioStep.TYPE_SQL =>
        SqlRequestService.getRequestById(readiness.targetId)
          .recover { case t: Throwable => ReadinessRequest(null, LogUtils.stackTraceToString(t)) }
          .map(req => ReadinessRequest(req))
      case _ =>
        Future.successful(ReadinessRequest(null, s"Unknown type: ${readiness.targetType}"))
    }
  }
}

object ReadinessCheckActor {

  def props(readiness: ReadinessCheck) = Props(new ReadinessCheckActor(readiness))

  val ERROR_RETRY_EXCEED = "Retry Exceed"

  final object DoCheck

  final case class ReadinessRequest(request: Any, errMsg: String = null)

  final object DoDelay

  final object DoRetry

}
