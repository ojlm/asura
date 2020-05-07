package asura.namerd

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

object NamerdConfig {

  implicit var system: ActorSystem = null
  implicit var dispatcher: ExecutionContext = null
  implicit var materializer: Materializer = null

  def init(
            system: ActorSystem,
            dispatcher: ExecutionContext,
            materializer: Materializer): Unit = {
    NamerdConfig.system = system
    NamerdConfig.dispatcher = dispatcher
    NamerdConfig.materializer = materializer
  }
}
