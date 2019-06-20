package asura.pea

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.ExecutionContext

object PeaConfig {

  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: ActorMaterializer = _

  var zkClient: CuratorFramework = null
  var zkPath: String = null
}
