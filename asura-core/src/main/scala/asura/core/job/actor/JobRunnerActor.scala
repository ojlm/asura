package asura.core.job.actor

import akka.actor.{ActorRef, Props, Status}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.LogUtils
import asura.core.CoreConfig
import asura.core.job.JobExecDesc
import asura.core.job.impl.RunCaseJob
import asura.core.runtime.RuntimeContext

import scala.concurrent.Future

/** Created by [[asura.core.job.actor.JobTestActor]] or [[asura.core.job.impl.RunCaseJob]]
  *
  * @param wsActor receive WebSocket message event
  */
class JobRunnerActor(wsActor: ActorRef) extends BaseActor {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_JOB_TIMEOUT
  val runtimeContext: RuntimeContext = RuntimeContext()

  override def receive: Receive = {
    case execDesc: JobExecDesc =>
      this.runtimeContext.options = execDesc.options
      for {
        _ <- runCases(execDesc)
        _ <- runScenarios(execDesc)
      } yield execDesc
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      log.warning(errLog)
  }

  private def runCases(execDesc: JobExecDesc): Future[JobExecDesc] = {
    //    val steps = execDesc.job.jobData.cs.map(doc => ScenarioStep(doc.id, ScenarioStep.TYPE_HTTP))
    //    if (steps.nonEmpty) {
    //      val scenarioActor = context.actorOf(ScenarioRunnerActor.props(StringUtils.EMPTY, false))
    //      scenarioActor ! SenderMessage(wsActor)
    //      val storeDataHelper = JobReportItemStoreDataHelper(
    //        execDesc.reportId,
    //        "c",
    //        execDesc.reportItemSaveActor,
    //        execDesc.jobId
    //      )
    //      val message = ScenarioTestJobMessage(
    //        JobRunnerActor.DEFAULT_SCENARIO_NAME,
    //        steps,
    //        storeDataHelper,
    //        this.runtimeContext
    //      )
    //      val future = (scenarioActor ? message).asInstanceOf[Future[ScenarioReportItemData]]
    //      future.map(scenarioReport => {
    //        execDesc
    //      })
    //    } else {
    //      Future.successful(execDesc)
    //    }
    RunCaseJob.doTestCase(execDesc, (log) => {
      if (null != wsActor) wsActor ! NotifyActorEvent(log)
    })
  }

  private def runScenarios(execDesc: JobExecDesc): Future[JobExecDesc] = {
    // TODO
    Future.successful(execDesc)
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobRunnerActor {

  def props(wsActor: ActorRef) = Props(new JobRunnerActor(wsActor))

  case object Finished

  val DEFAULT_SCENARIO_NAME = ""

}
