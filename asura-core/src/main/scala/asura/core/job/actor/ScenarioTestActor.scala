package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.util.LogUtils
import asura.core.ErrorMessages
import asura.core.actor.messages.SenderMessage
import asura.core.cs.ContextOptions
import asura.core.cs.scenario.ScenarioRunner
import asura.core.es.model.JobReportData.ScenarioReportItem
import asura.core.es.model.{Case, ScenarioStep}
import asura.core.es.service.CaseService
import asura.core.job.actor.ScenarioTestActor.ScenarioTestMessage

import scala.collection.mutable.ArrayBuffer

class ScenarioTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val executionContext = context.dispatcher
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case ScenarioTestMessage(summary, steps, options) =>
      val caseIds = steps.filter(ScenarioStep.TYPE_CASE == _.`type`).map(_.id)
      if (null != steps && steps.nonEmpty) {
        CaseService.getCasesByIdsAsMap(caseIds).map(caseIdMap => {
          val cases = ArrayBuffer[(String, Case)]()
          caseIds.foreach(id => {
            val value = caseIdMap.get(id)
            if (value.nonEmpty) {
              cases.append((id, value.get))
            }
          })
          ScenarioRunner.test("ScenarioTestActor", summary, cases, logMsg => {
            wsActor ! NotifyActorEvent(logMsg)
          }, options, logEvent => {
            wsActor ! logEvent
          }).pipeTo(self)
        })
      } else {
        wsActor ! ErrorActorEvent(ErrorMessages.error_EmptyCase.errMsg)
        wsActor ! PoisonPill
      }
    case report: ScenarioReportItem =>
      wsActor ! OverActorEvent(report)
      wsActor ! PoisonPill
    case eventMessage: ActorEvent =>
      wsActor ! eventMessage
    case Status.Failure(t) =>
      val logErrMsg = LogUtils.stackTraceToString(t)
      log.warning(logErrMsg)
      wsActor ! ErrorActorEvent(logErrMsg)
      wsActor ! PoisonPill
    case _ =>
      wsActor ! ErrorActorEvent(ErrorMessages.error_UnknownMessageType.errMsg)
      wsActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioTestActor {

  def props(user: String, out: ActorRef = null) = Props(new ScenarioTestActor(user, out))

  case class ScenarioTestMessage(summary: String, steps: Seq[ScenarioStep], options: ContextOptions)

}
