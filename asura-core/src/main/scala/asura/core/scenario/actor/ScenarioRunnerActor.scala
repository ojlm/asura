package asura.core.scenario.actor

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.pipe
import akka.util.Timeout
import asura.common.actor._
import asura.common.exceptions.WithDataException
import asura.common.util.{FutureUtils, LogUtils, StringUtils, XtermUtils}
import asura.core.assertion.engine.Statistic
import asura.core.dubbo.DubboReportModel.DubboRequestReportModel
import asura.core.dubbo.{DubboResult, DubboRunner}
import asura.core.es.model.JobReportData.{JobReportStepItemData, ReportStepItemStatus, ScenarioReportItemData}
import asura.core.es.model._
import asura.core.es.service.{DubboRequestService, HttpCaseRequestService, SqlRequestService}
import asura.core.http.{HttpRequestReportModel, HttpResult, HttpRunner}
import asura.core.job.actor.JobReportDataItemSaveActor.SaveReportDataHttpItemMessage
import asura.core.job.{JobReportItemResultEvent, JobReportItemStoreDataHelper}
import asura.core.runtime.{AbstractResult, ContextOptions, RuntimeContext}
import asura.core.scenario.actor.ScenarioRunnerActor.{ScenarioTestData, ScenarioTestJobMessage, ScenarioTestWebMessage}
import asura.core.sql.SqlReportModel.SqlRequestReportModel
import asura.core.sql.{SqlResult, SqlRunner}
import asura.core.{CoreConfig, ErrorMessages}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** alive during a scenario
  *
  * @param scenarioId
  * @param failFast if `true`, when a step is failed the left steps will be skipped, steps are relative
  */
class ScenarioRunnerActor(scenarioId: String, failFast: Boolean = true) extends BaseActor {

  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
  implicit val ec = context.dispatcher

  var steps: Seq[ScenarioStep] = Nil
  var stepsData: ScenarioTestData = null
  var runtimeContext: RuntimeContext = null
  val stepsReportItems = ArrayBuffer[JobReportStepItemData]()
  var scenarioReportItem = ScenarioReportItemData(scenarioId, null, stepsReportItems)

  // Actor which receive `asura.common.actor.ActorEvent` message.
  // Console log and step result will be sent to this actor which will influence the web ui
  // It should not be poisoned when running in a job
  var wsActor: ActorRef = null
  // Parent actor if this is running in a job
  var jobActor: ActorRef = null
  var storeHelper: JobReportItemStoreDataHelper = null

  var exports: Seq[VariablesExportItem] = Nil

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      // WebSocket actor for web console log
      wsActor = wsSender
      context.become(doTheTest())
  }

  private def doTheTest(): Receive = {
    case ScenarioTestWebMessage(summary, steps, options, imports, exports) =>
      this.scenarioReportItem.title = summary
      this.runtimeContext = RuntimeContext(options = options)
      this.runtimeContext.evaluateImportsVariables(imports)
      this.exports = exports
      this.steps = steps
      getScenarioTestData(steps).map(stepsData => {
        this.stepsData = stepsData
        self ! 0
      })
    case ScenarioTestJobMessage(summary, steps, storeHelper, runtimeContext, imports, exports) =>
      this.jobActor = sender()
      this.scenarioReportItem.title = summary
      this.runtimeContext = runtimeContext
      this.runtimeContext.evaluateImportsVariables(imports)
      this.steps = steps
      this.storeHelper = storeHelper
      this.exports = exports
      getScenarioTestData(steps).map(stepsData => {
        this.stepsData = stepsData
        self ! 0
      })
    case idx: Int =>
      if (idx < this.steps.length) {
        executeStep(idx) pipeTo self
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
            .map(httpResult => handleNormalResult(httpRequest.summary, httpResult, step, idx, httpRequest.exports))
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
            .map(dubboResult => handleNormalResult(dubboRequest.summary, dubboResult, step, idx, dubboRequest.exports))
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
            .map(sqlResult => handleNormalResult(sqlRequest.summary, sqlResult, step, idx, sqlRequest.exports))
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
      case _ => FutureUtils.requestFail(s"Unknown step type: ${step.`type`}")
    }
  }

  private def handleNormalResult(
                                  title: String,
                                  result: AbstractResult,
                                  step: ScenarioStep,
                                  idx: Int,
                                  exports: Seq[VariablesExportItem]
                                ): Int = {
    // assertion successful or failed
    val stepItemData = JobReportStepItemData.parse(title, result)
    if (null != storeHelper) {
      val itemDataId = s"${storeHelper.reportId}_${storeHelper.infix}_${idx}"
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
    val nextIdx = if (statis.isSuccessful) {
      if (this.failFast) {
        if (step.stored) {
          // extract all response data into the `_p` runtime context
          runtimeContext.setPrevContext(RuntimeContext.extractSelfContext(result))
        }
        // extract the exports into the runtime context
        runtimeContext.evaluateExportsVariables(exports)
      }
      idx + 1
    } else {
      this.scenarioReportItem.markFail()
      if (this.failFast) {
        skipLeftSteps(idx + 1)
        this.steps.length
      } else {
        idx + 1
      }
    }
    // send back result after render exports value
    if (null != wsActor) {
      val statusText = if (statis.isSuccessful) {
        XtermUtils.greenWrap(ReportStepItemStatus.STATUS_PASS)
      } else {
        XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL)
      }
      val msg = s"${consoleLogPrefix(step.`type`, idx)}${title} ${statusText}"
      wsActor ! NotifyActorEvent(msg)
      wsActor ! ItemActorEvent(JobReportItemResultEvent(idx, stepItemData.status, null, result))
    }
    nextIdx
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
      skipLeftSteps(idx + 1)
      this.steps.length
    } else {
      idx + 1
    }
  }

  private def skipLeftSteps(idx: Int): Unit = {
    for (i <- idx.until(this.steps.length)) {
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
      cs <- HttpCaseRequestService.getByIdsAsMap(csSeq)
      dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq)
      sql <- SqlRequestService.getByIdsAsMap(sqlSeq)
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
      s"[${idx + 1}] "
    }
    s"[SCN][${this.scenarioReportItem.title}][${XtermUtils.magentaWrap(formattedType)}]${formattedIdx}"
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioRunnerActor {

  def props(scenarioId: String, failFast: Boolean = true) = Props(new ScenarioRunnerActor(scenarioId, failFast))

  // from web
  case class ScenarioTestWebMessage(
                                     summary: String,
                                     steps: Seq[ScenarioStep],
                                     options: ContextOptions,
                                     imports: Seq[VariablesImportItem],
                                     exports: Seq[VariablesExportItem],
                                   )

  // from job
  case class ScenarioTestJobMessage(
                                     summary: String,
                                     steps: Seq[ScenarioStep],
                                     storeHelper: JobReportItemStoreDataHelper,
                                     runtimeContext: RuntimeContext,
                                     imports: Seq[VariablesImportItem],
                                     exports: Seq[VariablesExportItem],
                                   )

  case class ScenarioTestData(
                               http: Map[String, HttpCaseRequest],
                               dubbo: Map[String, DubboRequest],
                               sql: Map[String, SqlRequest],
                             )

}
