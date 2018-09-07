package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.util.StringUtils
import asura.core.cs.scenario.ScenarioRunner
import asura.core.es.model.Case
import asura.core.es.model.JobReportData.ScenarioReportItem
import asura.core.es.service.CaseService
import asura.core.job.actor.ScenarioTestActor.ScenarioTestMessage

import scala.collection.mutable.ArrayBuffer

class ScenarioTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val executionContext = context.dispatcher

  override def receive: Receive = {
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
          out ! NotifyActorEvent(logMsg)
        }).pipeTo(self)
      })
    case report: ScenarioReportItem =>
      out ! ItemActorEvent(report)
      out ! PoisonPill
    case eventMessage: ActorEvent =>
      out ! eventMessage
    case Status.Failure(t) =>
      out ! ErrorActorEvent(t.getMessage)
      out ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object ScenarioTestActor {

  def props(user: String, out: ActorRef) = Props(new ScenarioTestActor(user, out))

  case class ScenarioTestMessage(summary: String, cases: Seq[String])

}
