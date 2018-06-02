package asura.namerd

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

object NamerdConfig {

  var url: String = null
  implicit var system: ActorSystem = null
  implicit var dispatcher: ExecutionContext = null
  implicit var materializer: ActorMaterializer = null

  def init(url: String,
           system: ActorSystem,
           dispatcher: ExecutionContext,
           materializer: ActorMaterializer): Unit = {
    NamerdConfig.url = url
    NamerdConfig.system = system
    NamerdConfig.dispatcher = dispatcher
    NamerdConfig.materializer = materializer
  }
}
