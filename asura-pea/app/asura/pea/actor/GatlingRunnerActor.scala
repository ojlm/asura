package asura.pea.actor

import akka.actor.{Cancellable, Props}
import asura.common.actor.BaseActor
import asura.pea.PeaConfig
import asura.pea.actor.GatlingRunnerActor.StartMessage
import asura.pea.actor.PeaManagerActor.SingleHttpScenarioMessage
import asura.pea.simulation.SingleHttpSimulation
import io.gatling.app.PeaGatlingRunner
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class GatlingRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher
  val innerClassPath = getClass.getResource("/").getPath
  val singleHttpSimulationRef = classOf[SingleHttpSimulation].getCanonicalName

  override def receive: Receive = {
    case message: StartMessage =>
      sender() ! GatlingRunnerActor.start(message)
    case _: SingleHttpScenarioMessage =>
      val msg = StartMessage(innerClassPath, PeaConfig.resultsFolder, singleHttpSimulationRef)
      sender() ! GatlingRunnerActor.start(msg)
  }
}

object GatlingRunnerActor {

  case class StartMessage(binariesFolder: String, resultsFolder: String, simulationClass: String) {

    def toGatlingPropertiesMap: mutable.Map[String, _] = {
      val props = new GatlingPropertiesBuilder()
        .binariesDirectory(binariesFolder)
        .resultsDirectory(resultsFolder)
        .simulationClass(simulationClass)
      props.build
    }
  }

  def props() = Props(new GatlingRunnerActor())

  def start(message: StartMessage)(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    PeaGatlingRunner.run(message.toGatlingPropertiesMap)
  }

  case class PeaGatlingRunResult(runId: String, result: Future[GatlingResult], cancel: Cancellable)

  case class GatlingResult(code: Int, errMsg: String = null)

}
