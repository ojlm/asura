package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{FutureUtils, LogUtils, XtermUtils}
import asura.core.cs.{CaseContext, CaseResult, CaseRunner, ContextOptions}
import asura.core.dubbo.DubboResult
import asura.core.es.model.JobReportData.ScenarioReportItem
import asura.core.es.model._
import asura.core.es.service.{CaseService, DubboRequestService, SqlRequestService}
import asura.core.job.actor.ScenarioRunnerActor.{ScenarioTestData, ScenarioTestMessage}
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
  var scenarioReportItem = ScenarioReportItem(null, null)

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
      case ScenarioStep.TYPE_CASE =>
        val csOpt = this.stepsData.http.get(step.id)
        if (csOpt.nonEmpty) {
          val httpRequest = csOpt.get
          CaseRunner.test(step.id, httpRequest, CaseContext(options = this.contextOptions))
            .map(httpResult => {
              handleResult(httpRequest, httpResult)
              idx + 1
            })
        } else {
          Future.successful(idx + 1)
        }
      case ScenarioStep.TYPE_DUBBO =>
        val dubboOpt = this.stepsData.dubbo.get(step.id)
        if (dubboOpt.nonEmpty) {
          val dubboRequest = dubboOpt.get
          (this.dubboInvoker ? dubboRequest.toDubboGenericRequest).flatMap(dubboResponse => {
            DubboResult.evaluate(dubboRequest, dubboResponse.asInstanceOf[Object])
          }).map(dubboResult => {
            handleResult(dubboRequest, dubboResult)
            idx + 1
          })
        } else {
          Future.successful(idx + 1)
        }
      case ScenarioStep.TYPE_SQL =>
        val sqlOpt = this.stepsData.sql.get(step.id)
        if (sqlOpt.nonEmpty) {
          val sqlRequest = sqlOpt.get
          (this.sqlInvoker ? sqlRequest).flatMap(sqlResponse => {
            SqlResult.evaluate(sqlRequest, sqlResponse.asInstanceOf[Object])
          }).map(sqlResult => {
            handleResult(sqlRequest, sqlResult)
            idx + 1
          })
        } else {
          Future.successful(idx + 1)
        }
      case _ => FutureUtils.requestFail(s"Unknown step type: ${step.`type`}")
    }
  }

  private def handleResult(request: Case, result: CaseResult): Unit = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_CASE)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
  }

  private def handleResult(request: DubboRequest, result: DubboResult): Unit = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_DUBBO)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
  }

  private def handleResult(request: SqlRequest, result: SqlResult): Unit = {
    if (null != wsActor) {
      val msg = s"${consoleLogPrefix(ScenarioStep.TYPE_SQL)}${request.summary} ${result.statis.isSuccessful}"
      wsActor ! NotifyActorEvent(msg)
    }
  }

  private def getScenarioTestData(steps: Seq[ScenarioStep]): Future[ScenarioTestData] = {
    val csSeq = ArrayBuffer[String]()
    val dubboSeq = ArrayBuffer[String]()
    val sqlSeq = ArrayBuffer[String]()
    steps.foreach(step => {
      step.`type` match {
        case ScenarioStep.TYPE_CASE => csSeq += step.id
        case ScenarioStep.TYPE_DUBBO => dubboSeq += step.id
        case ScenarioStep.TYPE_SQL => sqlSeq += step.id
        case _ =>
      }
    })
    val res = for {
      cs <- CaseService.getByIdsAsMap(csSeq)
      dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq)
      sql <- SqlRequestService.getByIdsAsMap(sqlSeq)
    } yield (cs, dubbo, sql)
    res.map(triple => {
      if (null != wsActor) {
        val msg = s"${consoleLogPrefix("summary")}${XtermUtils.magentaWrap("http")}:${triple._1.size}, " +
          s"${XtermUtils.magentaWrap("dubbo")}:${triple._2.size}, " +
          s"${XtermUtils.magentaWrap("sql")}:${triple._3.size}"
        wsActor ! NotifyActorEvent(msg)
      }
      ScenarioTestData(triple._1, triple._2, triple._3)
    })
  }

  def consoleLogPrefix(stepType: String) = {
    s"scenario[${this.scenarioReportItem.title}]=> [${XtermUtils.magentaWrap(stepType)}] "
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioRunnerActor {

  def props() = Props(new ScenarioRunnerActor())

  case class ScenarioTestMessage(summary: String, steps: Seq[ScenarioStep], options: ContextOptions)

  case class ScenarioTestData(
                               http: Map[String, Case],
                               dubbo: Map[String, DubboRequest],
                               sql: Map[String, SqlRequest],
                             )

}
