package asura.ui

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import asura.ui.actor.DriverHolderActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class UiConfig(
                     system: ActorSystem,
                     ec: ExecutionContext,
                     taskListener: ActorRef,
                   )

object UiConfig {

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes

  var driverHolder: ActorRef = null

  def init(config: UiConfig): Unit = {
    val system = config.system
    val ec = config.ec
    driverHolder = system.actorOf(DriverHolderActor.props(config.taskListener, ec), "driver-holder")
  }

}
