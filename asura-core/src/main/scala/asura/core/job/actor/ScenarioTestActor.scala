package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.util.StringUtils
import asura.core.actor.messages.SenderMessage
import asura.core.cs.scenario.ScenarioRunner
import asura.core.es.model.Case
import asura.core.es.model.JobReportData.ScenarioReportItem
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

  def handleRequest(webActor: ActorRef): Receive = {
    case ScenarioTestMessage(summary, caseIds) =>
      CaseService.getCasesByIdsAsMap(caseIds).map(caseIdMap => {
        val cases = ArrayBuffer[(String, Case)]()
        caseIds.foreach(id => {
          val value = caseIdMap.get(id)
          if (value.nonEmpty) {
            cases.append((id, value.get))
          }
        })
        ScenarioRunner.test(StringUtils.EMPTY, summary, cases, logMsg => {
          webActor ! NotifyActorEvent(logMsg)
        }).pipeTo(self)
      })
    case report: ScenarioReportItem =>
      webActor ! ItemActorEvent(report)
      webActor ! PoisonPill
    case eventMessage: ActorEvent =>
      webActor ! eventMessage
    case Status.Failure(t) =>
      webActor ! ErrorActorEvent(t.getMessage)
      webActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioTestActor {

  def props(user: String, out: ActorRef = null) = Props(new ScenarioTestActor(user, out))

  case class ScenarioTestMessage(summary: String, cases: Seq[String])

}
