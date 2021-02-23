package asura.ui.cli

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

object CliSystem {

  val LOG_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"

  lazy implicit val ec = ExecutionContext.global

  lazy val system: ActorSystem = {
    ActorSystem("ui-cli")
  }

}
