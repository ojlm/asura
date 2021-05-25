package asura.ui.cli.codec

import asura.ui.cli.codec.WindowEventHandler.logger
import asura.ui.cli.hub.Hubs.ControllerHub
import com.typesafe.scalalogging.Logger
import javafx.event.{Event, EventHandler}
import javafx.scene.input.{KeyEvent, MouseEvent}

case class WindowEventHandler(device: String) extends EventHandler[Event] {

  val sinks = ControllerHub.getSinks(device)

  override def handle(event: Event): Unit = {
    event match {
      case event: MouseEvent =>
        logger.info(s"${event.getEventType.getName} (x: ${event.getX}, ${event.getY}, ${event.getZ}), ")
      case event: KeyEvent =>
        logger.info(s"${event.getEventType.getName} code: ${event.getCode}")
      case _ =>
        logger.info(s"${event.getEventType.getName}")
    }
  }

}

object WindowEventHandler {

  val logger = Logger(getClass)

}
