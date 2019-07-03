package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.{ask, pipe}
import asura.common.actor._
import asura.common.util.{LogUtils, StringUtils, XtermUtils}
import asura.core.es.model.JobReportData.{ReportStepItemStatus, ScenarioReportItemData}
import asura.core.es.model.ScenarioStep
import asura.core.es.service.ScenarioService
import asura.core.job.impl.RunCaseJob
import asura.core.job.{JobExecDesc, JobReportItemStoreDataHelper}
import asura.core.runtime.{ControllerOptions, RuntimeContext}
import asura.core.scenario.actor.ScenarioRunnerActor.ScenarioTestJobMessage
import asura.core.scenario.actor.{ScenarioRunnerActor, ScenarioStepBasicActor}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** Created by [[asura.core.job.actor.JobTestActor]] or [[asura.core.job.impl.RunCaseJob]].
  * This receive a `JobExecDesc` message and send it back after this is finished.
  *
  * @param wsActor    receive WebSocket message event
  * @param controller this value may be null
  */
class JobRunnerActor(var wsActor: ActorRef, controller: ControllerOptions) extends ScenarioStepBasicActor {

  var runtimeContext: RuntimeContext = RuntimeContext()

  var scenarioTestJobMessages: Seq[(ScenarioStep, ScenarioTestJobMessage)] = null
  var execDesc: JobExecDesc = null
  var resultReceiver: ActorRef = null
  val scenarioReports = ArrayBuffer[ScenarioReportItemData]()

  override def receive: Receive = {
    case execDesc: JobExecDesc =>
      this.execDesc = execDesc
      this.execDesc.report.data.scenarios = scenarioReports
      this.resultReceiver = sender()
      if (null != execDesc.overrideRuntime) this.runtimeContext = execDesc.overrideRuntime
      this.runtimeContext.options = execDesc.options
      this.runtimeContext.evaluateImportsVariables(execDesc.imports)
        .flatMap(_ => runCases(execDesc))
        .flatMap(_ => buildScenarioTestJobMessages(execDesc))
        .map(messages => {
          this.scenarioTestJobMessages = messages
          if (null != controller && controller.from > 0) {
            skipSteps(0, controller.from)
            self ! controller.from
          } else {
            self ! 0
          }
        })
    case idx: Int =>
      if (idx < 0) {
        // do nothing, waiting for message which >= 0
      } else if (idx < this.scenarioTestJobMessages.length) {
        if (null != controller && idx > controller.to) {
          skipSteps(idx, this.scenarioTestJobMessages.length)
          self ! this.scenarioTestJobMessages.length
        } else {
          runScenarioStep(idx) pipeTo self
        }
      } else {
        this.resultReceiver ! this.execDesc
        self ! PoisonPill
      }
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      log.warning(errLog)
      if (null != wsActor) {
        wsActor ! ErrorActorEvent(t.getMessage)
      }
  }

  // TODO: deprecate and refactor this implementation
  private def runCases(execDesc: JobExecDesc): Future[JobExecDesc] = {
    RunCaseJob.doTestCase(execDesc, (log) => {
      if (null != wsActor) wsActor ! NotifyActorEvent(log)
    })
  }

  private def runScenarioStep(idx: Int): Future[Int] = {
    this.runtimeContext.eraseScenarioData()
    val (step, message) = this.scenarioTestJobMessages(idx)
    step.`type` match {
      case ScenarioStep.TYPE_DELAY => handleDelayStep(step, idx)
      case ScenarioStep.TYPE_JUMP => handleJumpStep(step, idx)
      case _ =>
        if (message.steps.nonEmpty) {
          val scenarioActor = context.actorOf(ScenarioRunnerActor.props(step.id, true))
          scenarioActor ! SenderMessage(wsActor)
          message.storeHelper.jobLoopCount = this.loopCount
          val future = (scenarioActor ? message).asInstanceOf[Future[ScenarioReportItemData]]
          future.map(scenarioReport => {
            this.scenarioReports += scenarioReport
            if (!scenarioReport.isSuccessful()) {
              this.execDesc.report.result = JobExecDesc.STATUS_FAIL
            }
            idx + 1
          })
        } else {
          Future.successful(idx + 1)
        }
    }
  }

  override def consoleLogPrefix(stepType: String, idx: Int): String = {
    val formattedType = stepType match {
      case ScenarioStep.TYPE_DELAY => "DELAY"
      case ScenarioStep.TYPE_JUMP => "JUMP "
      case _ => "SCN  "
    }
    val formattedIdx = if (-1 == idx) {
      StringUtils.EMPTY
    } else {
      s"[${idx + 1}] "
    }
    s"[JOB][${execDesc.report.jobName}][${XtermUtils.magentaWrap(formattedType)}]${formattedIdx}"
  }

  override def sendJumpMsgAndGetJumpStep(expect: Int, step: ScenarioStep, idx: Int): Int = {
    val scenarioSteps = execDesc.job.jobData.scenario
    if (null != wsActor) {
      if (expect < 0 || expect > scenarioSteps.length - 1) {
        XtermUtils.redWrap(s"Can't jump to step [${expect}]")
      } else {
        val (_, stepMsg) = scenarioTestJobMessages(expect)
        val targetSummary = if (null != stepMsg) stepMsg.summary else StringUtils.EMPTY
        val jumpMsg = XtermUtils.blueWrap(s"Jump to step [${expect + 1}.${targetSummary}]")
        val msg = s"${consoleLogPrefix(step.`type`, idx)}${jumpMsg}"
        wsActor ! NotifyActorEvent(msg)
      }
    }
    if (expect < 0 || expect > scenarioSteps.length - 1) scenarioSteps.length else expect
  }

  def skipSteps(idx: Int, until: Int): Unit = {
    for (i <- idx.until(until)) {
      val (_, message) = scenarioTestJobMessages(i)
      val log = s"${consoleLogPrefix(null, idx)}[SCN] ${message.summary} ${
        XtermUtils.yellowWrap(ReportStepItemStatus.STATUS_SKIPPED)
      }"
      if (null != wsActor) wsActor ! NotifyActorEvent(log)
    }
  }

  private def buildScenarioTestJobMessages(execDesc: JobExecDesc): Future[Seq[(ScenarioStep, ScenarioTestJobMessage)]] = {
    val job = execDesc.job
    val scenarioSteps = job.jobData.scenario
    if (null != scenarioSteps && scenarioSteps.nonEmpty) {
      val scenarioIds = scenarioSteps.filter(_.isScenarioStep()).map(_.id)
      ScenarioService.getScenariosByIdsAsMap(scenarioIds).map(scenarioMap => {
        val messages = ArrayBuffer[(ScenarioStep, ScenarioTestJobMessage)]()
        for (i <- 0 until scenarioSteps.length) {
          val step = scenarioSteps(i)
          if (step.isScenarioStep()) {
            val scenario = scenarioMap.get(step.id).get
            val storeDataHelper = JobReportItemStoreDataHelper(execDesc.reportId, s"s${i.toString}", execDesc.reportItemSaveActor, execDesc.jobId)
            val message = ScenarioTestJobMessage(scenario.summary, scenario.steps, storeDataHelper, this.runtimeContext, scenario.imports, scenario.exports)
            messages += ((step, message))
          } else {
            messages += ((step, null))
          }
        }
        messages
      })
    } else {
      Future.successful(Nil)
    }
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobRunnerActor {

  def props(wsActor: ActorRef, controller: ControllerOptions) = Props(new JobRunnerActor(wsActor, controller))

  val DEFAULT_SCENARIO_NAME = "INNER"

}
