package asura.ui

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import asura.ui.actor.DriverHolderActor
import asura.ui.driver.UiDriverProvider
import asura.ui.model.ChromeDriverInfo

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class UiConfig(
                     system: ActorSystem,
                     ec: ExecutionContext,
                     taskListener: ActorRef,
                     localChrome: ChromeDriverInfo,
                     uiDriverProvider: UiDriverProvider,
                     syncInterval: Int,
                   )

object UiConfig {

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes

  var driverHolder: ActorRef = null

  def init(config: UiConfig): Unit = {
    val system = config.system
    val ec = config.ec
    driverHolder = system.actorOf(DriverHolderActor.props(
      config.localChrome, config.uiDriverProvider,
      config.taskListener, config.syncInterval, ec,
    ), "driver-holder")
  }

}
