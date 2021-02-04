package asura.ui.cli

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

object CliSystem {

  lazy implicit val ec = ExecutionContext.global

  lazy val system: ActorSystem = {
    ActorSystem("ui-cli")
  }

}
