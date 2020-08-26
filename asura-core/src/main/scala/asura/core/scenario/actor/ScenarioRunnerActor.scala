package asura.core.scenario.actor

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.exceptions.WithDataException
import asura.common.util.{FutureUtils, LogUtils, StringUtils, XtermUtils}
import asura.core.ErrorMessages
import asura.core.assertion.engine.Statistic
import asura.core.dubbo.DubboReportModel.DubboRequestReportModel
import asura.core.dubbo.{DubboResult, DubboRunner}
import asura.core.es.model.JobReportData.{JobReportStepItemData, ReportStepItemStatus, ScenarioReportItemData}
import asura.core.es.model._
import asura.core.es.service.{DubboRequestService, HttpCaseRequestService, SqlRequestService}
import asura.core.http.{HttpRequestReportModel, HttpResult, HttpRunner}
import asura.core.job.actor.JobReportDataItemSaveActor.SaveReportDataHttpItemMessage
import asura.core.job.{JobReportItemResultEvent, JobReportItemStoreDataHelper}
import asura.core.runtime.{AbstractResult, ContextOptions, ControllerOptions, RuntimeContext}
import asura.core.scenario.actor.ScenarioRunnerActor.{ScenarioTestData, ScenarioTestJobMessage, ScenarioTestWebMessage}
import asura.core.sql.SqlReportModel.SqlRequestReportModel
import asura.core.sql.{SqlResult, SqlRunner}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** alive during a scenario */
class ScenarioRunnerActor(scenarioId: String) extends ScenarioStepBasicActor {

  var failFast = true
  var runtimeContext: RuntimeContext = null
  var wsActor: ActorRef = null

  var steps: Seq[ScenarioStep] = Nil
  var stepsData: ScenarioTestData = null
  val stepsReportItems = ArrayBuffer[JobReportStepItemData]()
  var scenarioReportItem = ScenarioReportItemData(scenarioId, null, stepsReportItems)

  // Parent actor if this is running in a job
  var jobActor: ActorRef = null
  var storeHelper: JobReportItemStoreDataHelper = null

  var controller: ControllerOptions = null
  var exports: Seq[VariablesExportItem] = Nil

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      // WebSocket actor for web console log
      wsActor = wsSender
      context.become(doTheTest())
  }

  private def doTheTest(): Receive = {
    case ScenarioTestWebMessage(summary, steps, options, imports, exports, failFast, controller) =>
      this.scenarioReportItem.title = summary
      this.runtimeContext = RuntimeContext(options = options)
      this.controller = controller
      this.exports = exports
      this.steps = steps
      this.failFast = failFast
      this.runtimeContext.evaluateImportsVariables(imports)
        .flatMap(_ => getScenarioTestData(steps))
        .map(stepsData => {
          this.stepsData = stepsData
          if (null != controller && controller.from > 0) {
            skipSteps(0, controller.from)
            self ! controller.from
          } else {
            self ! 0
          }
        })
    case ScenarioTestJobMessage(summary, steps, storeHelper, runtimeContext, imports, exports, failFast) =>
      this.jobActor = sender()
      this.scenarioReportItem.title = summary
      this.runtimeContext = runtimeContext
      this.steps = steps
      this.storeHelper = storeHelper
      this.exports = exports
      this.failFast = failFast
      this.runtimeContext.evaluateImportsVariables(imports)
        .flatMap(_ => getScenarioTestData(steps))
        .map(stepsData => {
          this.stepsData = stepsData
          self ! 0
        })
    case idx: Int =>
      if (idx < 0) {
        // do nothing, waiting for message which >= 0
      } else if (idx < this.steps.length) {
        if (null != controller && idx > controller.to) {
          skipSteps(idx, this.steps.length)
          self ! this.steps.length
        } else {
          executeStep(idx) pipeTo self
        }
      } else {
        if (null != wsActor) {
          val msg = s"[SCN][${this.scenarioReportItem.title}] ${
            if (this.scenarioReportItem.isSuccessful())
              XtermUtils.greenWrap(this.scenarioReportItem.status)
            else
              XtermUtils.redWrap(this.scenarioReportItem.status)
          }"
          wsActor ! NotifyActorEvent(msg)
          wsActor ! OverActorEvent(this.scenarioReportItem)
          if (null == jobActor) wsActor ! Status.Success
        }
        if (null != jobActor) {
          // running in a job
          runtimeContext.evaluateExportsVariables(this.exports)
          this.scenarioReportItem.renderedExportDesc = runtimeContext.renderedExportsDesc(this.exports)
          jobActor ! this.scenarioReportItem
          context stop self
        }
      }
    case Status.Failure(t) =>
      val logErrMsg = LogUtils.stackTraceToString(t)
      log.warning(logErrMsg)
      if (null != wsActor) {
        wsActor ! ErrorActorEvent(t.getMessage)
        if (null == jobActor) wsActor ! Status.Success
      }
    case _ =>
      if (null != wsActor) {
        wsActor ! ErrorActorEvent(ErrorMessages.error_UnknownMessageType.errMsg)
        if (null == jobActor) wsActor ! Status.Success
      }
  }

  // execute step and return the next step index
  private def executeStep(idx: Int): Future[Int] = {
    val step = this.steps(idx)
    step.`type` match {
      case ScenarioStep.TYPE_HTTP =>
        val csOpt = this.stepsData.http.get(step.id)
        if (csOpt.nonEmpty) {
          val httpRequest = csOpt.get
          HttpRunner.test(step.id, httpRequest, this.runtimeContext)
            .flatMap(httpResult => handleNormalResult(httpRequest.summary, httpResult, step, idx, httpRequest.exports))
            .recover {
              case WithDataException(t, rendered) =>
                handleExceptionalResult(
                  httpRequest.summary,
                  HttpResult.exceptionResult(step.id, rendered.asInstanceOf[HttpRequestReportModel], this.runtimeContext.rawContext),
                  step, idx, t
                )
              case t: Throwable =>
                handleExceptionalResult(httpRequest.summary, HttpResult.exceptionResult(step.id), step, idx, t)
            }
        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_HTTP, step.id)
        }
      case ScenarioStep.TYPE_DUBBO =>
        val dubboOpt = this.stepsData.dubbo.get(step.id)
        if (dubboOpt.nonEmpty) {
          val dubboRequest = dubboOpt.get
          DubboRunner.test(step.id, dubboRequest, this.runtimeContext)
            .flatMap(dubboResult => handleNormalResult(dubboRequest.summary, dubboResult, step, idx, dubboRequest.exports))
            .recover {
              case WithDataException(t, rendered) =>
                handleExceptionalResult(
                  dubboRequest.summary,
                  DubboResult.exceptionResult(step.id, rendered.asInstanceOf[DubboRequestReportModel], this.runtimeContext.rawContext),
                  step, idx, t)
              case t: Throwable =>
                handleExceptionalResult(dubboRequest.summary, DubboResult.exceptionResult(step.id), step, idx, t)
            }
        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_DUBBO, step.id)
        }
      case ScenarioStep.TYPE_SQL =>
        val sqlOpt = this.stepsData.sql.get(step.id)
        if (sqlOpt.nonEmpty) {
          val sqlRequest = sqlOpt.get
          SqlRunner.test(step.id, sqlRequest, this.runtimeContext)
            .flatMap(sqlResult => handleNormalResult(sqlRequest.summary, sqlResult, step, idx, sqlRequest.exports))
            .recover {
              case WithDataException(t, rendered) =>
                handleExceptionalResult(
                  sqlRequest.summary,
                  SqlResult.exceptionResult(step.id, rendered.asInstanceOf[SqlRequestReportModel], this.runtimeContext.rawContext),
                  step, idx, t)
              case t: Throwable =>
                handleExceptionalResult(sqlRequest.summary, SqlResult.exceptionResult(step.id), step, idx, t)
            }
        } else {
          handleEmptyStepData(idx, ScenarioStep.TYPE_SQL, step.id)
        }
      case ScenarioStep.TYPE_DELAY => handleDelayStep(step, idx)
      case ScenarioStep.TYPE_JUMP => handleJumpStep(step, idx)
      case _ => FutureUtils.requestFail(s"Unknown step type: ${step.`type`}")
    }
  }


  override def sendJumpMsgAndGetJumpStep(expect: Int, step: ScenarioStep, idx: Int): Int = {
    if (null != wsActor) {
      val jumpMsg = if (expect < 0 || expect > this.steps.length - 1) {
        XtermUtils.redWrap(s"Can't jump to step [${expect}]")
      } else {
        val targetSummary = getStepSummary(this.steps(expect))
        XtermUtils.blueWrap(s"Jump to step [${expect + 1}.${targetSummary}]")
      }
      val msg = s"${consoleLogPrefix(step.`type`, idx)}${jumpMsg}"
      wsActor ! NotifyActorEvent(msg)
    }
    if (expect < 0 || expect > this.steps.length - 1) this.steps.length else expect
  }

  private def handleNormalResult(
                                  title: String,
                                  result: AbstractResult,
                                  step: ScenarioStep,
                                  idx: Int,
                                  exports: Seq[VariablesExportItem]
                                ): Future[Int] = {
    // assertion successful or failed
    val stepItemData = JobReportStepItemData.parse(title, result)
    if (null != storeHelper) {
      val itemDataId = storeHelper.generateItemId(idx, loopCount)
      stepItemData.itemId = itemDataId
      val dataItem = JobReportDataItem.parse(
        storeHelper.jobId,
        storeHelper.reportId,
        scenarioId,
        step.`type`,
        result
      )
      storeHelper.actorRef ! SaveReportDataHttpItemMessage(itemDataId, dataItem)
    }
    this.stepsReportItems += stepItemData
    val statis = stepItemData.statis

    def sendCurrentToWsActor(statusStr: String): Unit = {
      if (null != wsActor) {
        val msg = s"${consoleLogPrefix(step.`type`, idx)}${title} ${statusStr}"
        wsActor ! NotifyActorEvent(msg)
        wsActor ! ItemActorEvent(JobReportItemResultEvent(idx, stepItemData.status, null, result))
      }
    }

    if (statis.isSuccessful) {
      if (this.failFast) {
        // extract the exports into the runtime context asynchronously
        runtimeContext.evaluateExportsVariables(exports).map(_ => {
          result.renderedExportDesc = runtimeContext.renderedExportsDesc(exports)
          sendCurrentToWsActor(XtermUtils.greenWrap(ReportStepItemStatus.STATUS_PASS))
          idx + 1
        })
      } else {
        Future.successful {
          sendCurrentToWsActor(XtermUtils.greenWrap(ReportStepItemStatus.STATUS_PASS))
          idx + 1
        }
      }
    } else {
      Future.successful {
        this.scenarioReportItem.markFail()
        if (this.failFast) {
          sendCurrentToWsActor(XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL))
          skipSteps(idx + 1, this.steps.length)
          this.steps.length
        } else {
          sendCurrentToWsActor(XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL))
          idx + 1
        }
      }
    }
  }

  private def handleExceptionalResult(
                                       title: String,
                                       failResult: AbstractResult,
                                       step: ScenarioStep,
                                       idx: Int,
                                       t: Throwable): Int = {
    // exception occurred
    val errorStack = LogUtils.stackTraceToString(t)
    val stepItemData = JobReportStepItemData.parse(title, failResult, msg = errorStack)
    this.stepsReportItems += stepItemData
    if (null != wsActor) {
      val statusText = s"${XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL)}"
      wsActor ! NotifyActorEvent(s"${consoleLogPrefix(step.`type`, idx)}${title} ${statusText}")
      wsActor ! NotifyActorEvent(s"${consoleLogPrefix(step.`type`, idx)}${title} ${errorStack}")
      wsActor ! ItemActorEvent(JobReportItemResultEvent(idx, stepItemData.status, errorStack, failResult))
    }
    this.scenarioReportItem.markFail()
    if (this.failFast) {
      skipSteps(idx + 1, this.steps.length)
      this.steps.length
    } else {
      idx + 1
    }
  }

  private def skipSteps(idx: Int, until: Int): Unit = {
    for (i <- idx.until(until)) {
      val step = this.steps(i)
      val title = step.`type` match {
        case ScenarioStep.TYPE_HTTP =>
          val csOpt = this.stepsData.http.get(step.id)
          csOpt.get.summary
        case ScenarioStep.TYPE_DUBBO =>
          val dubboOpt = this.stepsData.dubbo.get(step.id)
          dubboOpt.get.summary
        case ScenarioStep.TYPE_SQL =>
          val sqlOpt = this.stepsData.sql.get(step.id)
          sqlOpt.get.summary
        case _ => StringUtils.EMPTY
      }
      val itemData = JobReportStepItemData(step.id, title, null, Statistic(), step.`type`)
      itemData.status = ReportStepItemStatus.STATUS_SKIPPED
      this.stepsReportItems += itemData
      if (null != wsActor) {
        val msg = s"${consoleLogPrefix(step.`type`, i)}${title} ${
          XtermUtils.yellowWrap(ReportStepItemStatus.STATUS_SKIPPED)
        }"
        wsActor ! NotifyActorEvent(msg)
        wsActor ! ItemActorEvent(JobReportItemResultEvent(i, ReportStepItemStatus.STATUS_SKIPPED, null, null))
      }
    }
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
      cs <- HttpCaseRequestService.getByIdsAsMap(csSeq.toSeq)
      dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq.toSeq)
      sql <- SqlRequestService.getByIdsAsMap(sqlSeq.toSeq)
    } yield (cs, dubbo, sql)
    res.map(triple => {
      if (null != wsActor) {
        val msg = s"${consoleLogPrefix("SUM  ", -1)} " +
          s"${XtermUtils.magentaWrap("HTTP")}:${triple._1.size}, " +
          s"${XtermUtils.magentaWrap("DUBBO")}:${triple._2.size}, " +
          s"${XtermUtils.magentaWrap("SQL")}:${triple._3.size}"
        wsActor ! NotifyActorEvent(msg)
      }
      ScenarioTestData(triple._1, triple._2, triple._3)
    })
  }

  override def consoleLogPrefix(stepType: String, idx: Int) = {
    val formattedType = stepType match {
      case ScenarioStep.TYPE_HTTP => "HTTP "
      case ScenarioStep.TYPE_DUBBO => "DUBBO"
      case ScenarioStep.TYPE_SQL => "SQL  "
      case ScenarioStep.TYPE_DELAY => "DELAY"
      case ScenarioStep.TYPE_JUMP => "JUMP "
      case _ => stepType
    }
    val formattedIdx = if (-1 == idx) {
      StringUtils.EMPTY
    } else {
      s"[${idx + 1}] "
    }
    s"[SCN][${this.scenarioReportItem.title}][${XtermUtils.magentaWrap(formattedType)}]${formattedIdx}"
  }

  private def getStepSummary(step: ScenarioStep): String = {
    val docOpt = step.`type` match {
      case ScenarioStep.TYPE_HTTP => this.stepsData.http.get(step.id)
      case ScenarioStep.TYPE_SQL => this.stepsData.sql.get(step.id)
      case ScenarioStep.TYPE_DUBBO => this.stepsData.dubbo.get(step.id)
      case _ => None
    }
    if (docOpt.nonEmpty) {
      docOpt.get.summary
    } else {
      StringUtils.EMPTY
    }
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioRunnerActor {

  def props(scenarioId: String) = Props(new ScenarioRunnerActor(scenarioId))

  // from web
  case class ScenarioTestWebMessage(
                                     summary: String,
                                     steps: Seq[ScenarioStep],
                                     options: ContextOptions,
                                     imports: Seq[VariablesImportItem],
                                     exports: Seq[VariablesExportItem],
                                     failFast: Boolean = true,
                                     controller: ControllerOptions = null
                                   )

  // from job
  case class ScenarioTestJobMessage(
                                     summary: String,
                                     steps: Seq[ScenarioStep],
                                     storeHelper: JobReportItemStoreDataHelper,
                                     runtimeContext: RuntimeContext,
                                     imports: Seq[VariablesImportItem],
                                     exports: Seq[VariablesExportItem],
                                     failFast: Boolean = true,
                                   )

  case class ScenarioTestData(
                               http: Map[String, HttpCaseRequest],
                               dubbo: Map[String, DubboRequest],
                               sql: Map[String, SqlRequest],
                             )

}
