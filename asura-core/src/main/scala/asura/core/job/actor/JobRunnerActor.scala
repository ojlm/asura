package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{LogUtils, XtermUtils}
import asura.core.CoreConfig
import asura.core.es.model.JobReportData.{ReportStepItemStatus, ScenarioReportItemData}
import asura.core.es.service.ScenarioService
import asura.core.job.impl.RunCaseJob
import asura.core.job.{JobExecDesc, JobReportItemStoreDataHelper}
import asura.core.runtime.{ControllerOptions, RuntimeContext}
import asura.core.scenario.actor.ScenarioRunnerActor
import asura.core.scenario.actor.ScenarioRunnerActor.ScenarioTestJobMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** Created by [[asura.core.job.actor.JobTestActor]] or [[asura.core.job.impl.RunCaseJob]].
  * This receive a `JobExecDesc` message and send it back after this is finished.
  *
  * @param wsActor    receive WebSocket message event
  * @param controller this value may be null
  */
class JobRunnerActor(wsActor: ActorRef, controller: ControllerOptions) extends BaseActor {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_JOB_TIMEOUT
  val runtimeContext: RuntimeContext = RuntimeContext()
  var scenarioTestJobMessages: Seq[(String, ScenarioTestJobMessage)] = null
  var execDesc: JobExecDesc = null
  var resultReceiver: ActorRef = null
  val scenarioReports = ArrayBuffer[ScenarioReportItemData]()

  override def receive: Receive = {
    case execDesc: JobExecDesc =>
      this.execDesc = execDesc
      this.execDesc.report.data.scenarios = scenarioReports
      this.resultReceiver = sender()
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
      if (idx < this.scenarioTestJobMessages.length) {
        if (null != controller && idx > controller.to) {
          skipSteps(idx, this.scenarioTestJobMessages.length)
          self ! this.scenarioTestJobMessages.length
        } else {
          runScenario(idx) pipeTo self
        }
      } else {
        this.resultReceiver ! this.execDesc
        self ! PoisonPill
      }
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      log.warning(errLog)
  }

  // TODO: deprecate and refactor this implementation
  private def runCases(execDesc: JobExecDesc): Future[JobExecDesc] = {
    RunCaseJob.doTestCase(execDesc, (log) => {
      if (null != wsActor) wsActor ! NotifyActorEvent(log)
    })
  }

  private def runScenario(idx: Int): Future[Int] = {
    val (scenarioId, message) = this.scenarioTestJobMessages(idx)
    if (message.steps.nonEmpty) {
      val scenarioActor = context.actorOf(ScenarioRunnerActor.props(scenarioId, true))
      scenarioActor ! SenderMessage(wsActor)
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

  private def skipSteps(idx: Int, until: Int): Unit = {
    for (i <- idx.until(until)) {
      val (_, message) = this.scenarioTestJobMessages(i)
      val log = s"[JOB][${execDesc.report.jobName}][SCN] ${message.summary} ${
        XtermUtils.yellowWrap(ReportStepItemStatus.STATUS_SKIPPED)
      }"
      if (null != wsActor) wsActor ! NotifyActorEvent(log)
    }
  }

  private def buildScenarioTestJobMessages(execDesc: JobExecDesc): Future[Seq[(String, ScenarioTestJobMessage)]] = {
    val job = execDesc.job
    val scenarioDocs = job.jobData.scenario
    if (null != scenarioDocs && scenarioDocs.nonEmpty) {
      val scenarioIds = scenarioDocs.map(_.id)
      ScenarioService.getScenariosByIds(scenarioIds).map(list => {
        val map = mutable.Map[String, ScenarioTestJobMessage]()
        for (i <- 0 until list.length) {
          val (scenarioId, scenario) = list(i)
          val storeDataHelper = JobReportItemStoreDataHelper(execDesc.reportId, s"s${i.toString}", execDesc.reportItemSaveActor, execDesc.jobId)
          val message = ScenarioTestJobMessage(scenario.summary, scenario.steps, storeDataHelper, this.runtimeContext, scenario.imports, scenario.exports)
          map += (scenarioId -> message)
        }
        scenarioIds.map(id => ((id, map.get(id).get)))
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
