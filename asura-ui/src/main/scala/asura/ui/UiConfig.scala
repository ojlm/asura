package asura.ui

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import asura.ui.actor.ChromeDriverHolderActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class UiConfig(
                     system: ActorSystem,
                     ec: ExecutionContext,
                   )

object UiConfig {

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes

  var driverHolder: ActorRef = null

  def init(config: UiConfig): Unit = {
    val system = config.system
    val ec = config.ec
    driverHolder = system.actorOf(ChromeDriverHolderActor.props(ec), "chrome-driver-holder")
  }

}