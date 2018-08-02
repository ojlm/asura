package asura

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object GlobalImplicits {
  implicit val system = ActorSystem("asura-system")
  implicit val dispatcher = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

object Asura {

}
