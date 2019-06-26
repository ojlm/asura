package asura.pea.actor

import akka.actor.Props
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.pea.ErrorMessages
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaManagerActor.{GetNodeStatusMessage, SingleHttpScenarioMessage}
import asura.pea.model.{Injection, SingleRequest}

import scala.concurrent.Future

class PeaManagerActor extends BaseActor {

  var status = PeaManagerActor.NODE_STATUS_IDLE

  implicit val ec = context.dispatcher
  val zincCompilerActor = context.actorOf(ZincCompilerActor.props())
  val gatlingRunnerActor = context.actorOf(GatlingRunnerActor.props())

  override def receive: Receive = {
    case GetNodeStatusMessage => sender() ! status
    case message: SingleHttpScenarioMessage => doSingleHttpScenario(message) pipeTo sender()
    case _ => ErrorMessages.error_InvalidRequestParameters.toFutureFail pipeTo sender()
  }

  def doSingleHttpScenario(message: SingleHttpScenarioMessage): Future[Int] = {
    if (PeaManagerActor.NODE_STATUS_IDLE.equals(status)) {
      asura.pea.singleHttpScenario = message
      status = PeaManagerActor.NODE_STATUS_BUSY
      (gatlingRunnerActor ? message).asInstanceOf[Future[Int]].map(code => {
        status = PeaManagerActor.NODE_STATUS_IDLE
        code
      })
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }
}

object PeaManagerActor {

  val NODE_STATUS_IDLE = "idle"
  val NODE_STATUS_BUSY = "busy"

  def props() = Props(new PeaManagerActor())

  case object GetNodeStatusMessage

  case class SingleHttpScenarioMessage(
                                        var name: String,
                                        var request: SingleRequest,
                                        var injections: Seq[Injection],
                                      )

}

