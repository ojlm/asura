package asura.pea.actor

import java.nio.charset.StandardCharsets
import java.util.Date

import akka.actor.{Cancellable, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.JsonUtils
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.GatlingRunnerActor.PeaGatlingRunResult
import asura.pea.actor.PeaManagerActor.{GetNodeStatusMessage, SingleHttpScenarioMessage, StopEngine}
import asura.pea.model.{Injection, MemberStatus, SingleRequest}
import asura.pea.{ErrorMessages, PeaConfig}

import scala.concurrent.Future

class PeaManagerActor extends BaseActor {

  var memberStatus = MemberStatus(PeaManagerActor.NODE_STATUS_IDLE)

  implicit val ec = context.dispatcher
  val zincCompilerActor = context.actorOf(ZincCompilerActor.props())
  val gatlingRunnerActor = context.actorOf(GatlingRunnerActor.props())
  var engineCancelable: Cancellable = null

  override def receive: Receive = {
    case msg: MemberStatus => // send from listener
      log.debug(s"Current node data change to: ${msg}")
    case GetNodeStatusMessage =>
      sender() ! memberStatus
    case msg: SingleHttpScenarioMessage =>
      doSingleHttpScenario(msg) pipeTo sender()
    case StopEngine =>
      sender() ! tryStopEngine()
    case _ =>
      ErrorMessages.error_InvalidRequestParameters.toFutureFail pipeTo sender()
  }

  def tryStopEngine(): Boolean = {
    var result = true
    if (null != engineCancelable && !PeaManagerActor.NODE_STATUS_IDLE.equals(memberStatus.status)) {
      if (!engineCancelable.isCancelled) {
        if (engineCancelable.cancel()) {
          memberStatus.errMsg = "canceled"
        } else {
          memberStatus.errMsg = "cancel failed"
          result = false
        }
        updateEndStatus()
      }
    }
    result
  }

  def doSingleHttpScenario(message: SingleHttpScenarioMessage): Future[String] = {
    if (PeaManagerActor.NODE_STATUS_IDLE.equals(memberStatus.status)) {
      asura.pea.singleHttpScenario = message
      val futureRunResult = (gatlingRunnerActor ? message).asInstanceOf[Future[PeaGatlingRunResult]]
      futureRunResult.map(runResult => {
        engineCancelable = runResult.cancel
        runResult.result.map(result => {
          memberStatus.code = result.code
          memberStatus.errMsg = result.errMsg
          updateEndStatus()
        })
        updateRunningStatus(runResult.runId)
        runResult.runId
      })
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }

  private def updateEndStatus(): Unit = {
    memberStatus.status = PeaManagerActor.NODE_STATUS_IDLE
    memberStatus.end = new Date().getTime
    pushToZk()
  }

  private def updateRunningStatus(runId: String): Unit = {
    memberStatus.status = PeaManagerActor.NODE_STATUS_RUNNING
    memberStatus.runId = runId
    memberStatus.start = new Date().getTime
    pushToZk()
  }

  private def pushToZk(): Unit = {
    PeaConfig.zkClient
      .setData()
      .forPath(PeaConfig.zkCurrPath, JsonUtils.stringify(memberStatus).getBytes(StandardCharsets.UTF_8))
  }
}

object PeaManagerActor {

  val NODE_STATUS_IDLE = "idle"
  val NODE_STATUS_RUNNING = "running"

  def props() = Props(new PeaManagerActor())

  case object GetNodeStatusMessage

  case object StopEngine

  case class SingleHttpScenarioMessage(
                                        var name: String,
                                        var request: SingleRequest,
                                        var injections: Seq[Injection],
                                      )

}

