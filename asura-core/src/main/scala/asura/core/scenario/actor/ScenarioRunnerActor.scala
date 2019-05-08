package asura.core.scenario.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{FutureUtils, LogUtils, StringUtils, XtermUtils}
import asura.core.dubbo.DubboResult
import asura.core.es.model.JobReportData.ScenarioReportItemData
import asura.core.es.model._
import asura.core.es.service.{HttpCaseRequestService, DubboRequestService, SqlRequestService}
import asura.core.http.{HttpResult, HttpRunner}
import asura.core.runtime.{ContextOptions, RuntimeContext}
import asura.core.scenario.actor.ScenarioRunnerActor.{ScenarioTestData, ScenarioTestMessage}
import asura.core.sql.SqlResult
import asura.core.{ErrorMessages, RunnerActors}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

// alive during a scenario
class ScenarioRunnerActor() extends BaseActor {

  implicit val timeout: Timeout = 30.seconds
  implicit val ec = context.dispatcher

  val dubboInvoker = RunnerActors.dubboInvoker
  val sqlInvoker = RunnerActors.sqlInvoker

  var steps: Seq[ScenarioStep] = Nil
  var stepsData: ScenarioTestData = null
  var contextOptions: ContextOptions = null
  var scenarioReportItem = ScenarioReportItemData(null, null)

  // Actor which receive `asura.common.actor.ActorEvent` message for WebSocket api
  var wsActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      wsActor = wsSender
      context.become(handlerWsRequest())
  }

  private def handlerWsRequest(): Receive = {
    case ScenarioTestMessage(summary, steps, options) =>
      scenarioReportItem.title = summary
      contextOptions = options
      this.steps = steps
      getScenarioTestData(steps).map(stepsData => {
        this.stepsData = stepsData
        self ! 0
      })
    case idx: Int =>
      if (idx < this.steps.length) {
        executeStep(idx) pipeTo self
      } else {
        wsActor ! OverActorEvent(this.scenarioReportItem)
        wsActor ! PoisonPill
      }
    case Status.Failure(t) =>
      val logErrMsg = LogUtils.stackTraceToString(t)
      log.warning(logErrMsg)
      wsActor ! ErrorActorEvent(t.getMessage)
      wsActor ! PoisonPill
    case _ =>
      wsActor ! ErrorActorEvent(ErrorMessages.error_UnknownMessageType.errMsg)
      wsActor ! PoisonPill
  }

  // execute step and return next step index
  private def executeStep(idx: Int): Future[Int] = {
    val step = this.steps(idx)
    step.`type` match {
      case ScenarioStep.TYPE_HTTP =>
        val csOpt = this.stepsData.http.get(step.id)
        if (csOpt.nonEmpty) {
          val httpRequest = csOpt.get
          HttpRunner.test(step.id, httpRequest, RuntimeContext(options = this.contextOptions))
            .map(httpResult => handleSuccessResult(httpRequest, httpResult, idx))
            .recover {
              case t: Throwable => handleFailureResult(httpRequest, idx, t)
            }
        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_HTTP, step.id)
        }
      case ScenarioStep.TYPE_DUBBO =>
        val dubboOpt = this.stepsData.dubbo.get(step.id)
        if (dubboOpt.nonEmpty) {
          val dubboRequest = dubboOpt.get
          (this.dubboInvoker ? dubboRequest.toDubboGenericRequest).flatMap(dubboResponse => {
            DubboResult.evaluate(dubboRequest, dubboResponse.asInstanceOf[Object])
          }).map(dubboResult => handleSuccessResult(dubboRequest, dubboResult, idx))
            .recover {
              case t: Throwable => handleFailureResult(dubboRequest, idx, t)
            }

        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_DUBBO, step.id)
        }
      case ScenarioStep.TYPE_SQL =>
        val sqlOpt = this.stepsData.sql.get(step.id)
        if (sqlOpt.nonEmpty) {
          val sqlRequest = sqlOpt.get
          (this.sqlInvoker ? sqlRequest).flatMap(sqlResponse => {
            SqlResult.evaluate(sqlRequest, sqlResponse.asInstanceOf[Object])
          }).map(sqlResult => handleSuccessResult(sqlRequest, sqlResult, idx))
            .recover {
              case t: Throwable => handleFailureResult(sqlRequest, idx, t)
            }
        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_SQL, step.id)
        }
      case _ => FutureUtils.requestFail(s"Unknown step type: ${step.`type`}")
    }
  }

  private def handleSuccessResult(request: HttpCaseRequest, result: HttpResult, idx: Int): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_HTTP, idx)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleFailureResult(request: HttpCaseRequest, idx: Int, t: Throwable): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_HTTP, idx)}${request.summary} fail"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleSuccessResult(request: DubboRequest, result: DubboResult, idx: Int): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_DUBBO, idx)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleFailureResult(request: DubboRequest, idx: Int, t: Throwable): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_DUBBO, idx)}${request.summary} fail"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleSuccessResult(request: SqlRequest, result: SqlResult, idx: Int): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_SQL, idx)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleFailureResult(request: SqlRequest, idx: Int, t: Throwable): Int = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_SQL, idx)}${request.summary} fail"
      wsActor ! NotifyActorEvent(msg)
    }
    idx + 1
  }

  private def handleEmptyStepData(idx: Int, stepType: String, docId: String): Future[Int] = {
    // this should not be called for now
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(stepType, idx)}${XtermUtils.redWrap(s"Empty id: ${docId}")}"
      wsActor ! NotifyActorEvent(msg)
    }
    Future.successful(idx + 1)
  }

  private def getScenarioTestData(steps: Seq[ScenarioStep]): Future[ScenarioTestData] = {
    val csSeq = ArrayBuffer[String]()
    val dubboSeq = ArrayBuffer[String]()
    val sqlSeq = ArrayBuffer[String]()
    steps.foreach(step => {
      step.`type` match {
        case ScenarioStep.TYPE_HTTP => csSeq += step.id
        case ScenarioStep.TYPE_DUBBO => dubboSeq += step.id
        case ScenarioStep.TYPE_SQL => sqlSeq += step.id
        case _ =>
      }
    })
    val res = for {
      cs <- HttpCaseRequestService.getByIdsAsMap(csSeq)
      dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq)
      sql <- SqlRequestService.getByIdsAsMap(sqlSeq)
    } yield (cs, dubbo, sql)
    res.map(triple => {
      if (null != wsActor) {
        val msg = s"\n\n${consoleLogPrefix("SUM  ", -1)} " +
          s"${XtermUtils.magentaWrap("HTTP")}:${triple._1.size}, " +
          s"${XtermUtils.magentaWrap("DUBBO")}:${triple._2.size}, " +
          s"${XtermUtils.magentaWrap("SQL")}:${triple._3.size}\n"
        wsActor ! NotifyActorEvent(msg)
      }
      ScenarioTestData(triple._1, triple._2, triple._3)
    })
  }

  def consoleLogPrefix(stepType: String, idx: Int) = {
    val formattedType = stepType match {
      case ScenarioStep.TYPE_HTTP => "HTTP "
      case ScenarioStep.TYPE_DUBBO => "DUBBO"
      case ScenarioStep.TYPE_SQL => "SQL  "
      case _ => stepType
    }
    val formattedIdx = if (-1 == idx) {
      StringUtils.EMPTY
    } else {
      s"[${idx}] "
    }
    s"[SCN][${this.scenarioReportItem.title}][${XtermUtils.magentaWrap(formattedType)}]${formattedIdx}"
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioRunnerActor {

  def props() = Props(new ScenarioRunnerActor())

  case class ScenarioTestMessage(summary: String, steps: Seq[ScenarioStep], options: ContextOptions)

  case class ScenarioTestData(
                               http: Map[String, HttpCaseRequest],
                               dubbo: Map[String, DubboRequest],
                               sql: Map[String, SqlRequest],
                             )

}
