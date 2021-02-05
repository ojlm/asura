package asura.ui

import java.util

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import asura.ui.actor.ChromeDriverHolderActor
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
                     options: util.HashMap[String, Object] = null,
                   )

object UiConfig {

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10.minutes

  var localChromeDriver: ActorRef = null

  def init(config: UiConfig): Unit = {
    val system = config.system
    val ec = config.ec
    localChromeDriver = system.actorOf(
      ChromeDriverHolderActor.props(
        config.localChrome, config.uiDriverProvider,
        config.taskListener, config.syncInterval,
        config.options, ec,
      ),
      "driver-holder",
    )
  }

}
