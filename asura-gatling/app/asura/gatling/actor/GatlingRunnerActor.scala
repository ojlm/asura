package asura.gatling.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.gatling.actor.GatlingRunnerActor.StartMessage
import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.collection.mutable

class GatlingRunnerActor extends BaseActor {

  override def receive: Receive = {
    case message: StartMessage => GatlingRunnerActor.start(message)
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

  def start(message: StartMessage): Int = {
    Gatling.fromMap(message.toGatlingPropertiesMap)
  }

}
