package asura.ui.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{ActorEvent, BaseActor, SenderMessage}
import asura.common.model.ApiCode
import asura.ui.UiConfig
import asura.ui.actor.ChromeDriverHolderActor.{SubscribeCommandLogMessage, SubscribeDriverDevToolsEventMessage, SubscribeDriverStatusMessage}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver.{DriverCommand, DriverCommandStart}

class WebControllerActor(username: String) extends BaseActor {

  // represent websocket connection
  var wsActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      wsActor = wsSender
      subscribeDriverEvent()
    case command: DriverCommand =>
      command.creator = username
      sendCommand(command)
    case msg: DriverCommandStart =>
      wsActor ! newEvent(WebControllerActor.COMMAND_START, msg)
    case PublishCommandLogMessage(_, log) =>
      wsActor ! newEvent(WebControllerActor.COMMAND_LOG, log)
    case PublishDriverStatusMessage(_, status) =>
      wsActor ! newEvent(WebControllerActor.DRIVER_STATUS, status)
    case PublishDriverDevToolsMessage(_, msg) =>
      wsActor ! newEvent(WebControllerActor.DRIVER_LOG, msg)
  }

  private def sendCommand(command: DriverCommand): Unit = {
    if (UiConfig.driverHolder != null) {
      UiConfig.driverHolder ! command
    }
  }

  private def subscribeDriverEvent(): Unit = {
    if (UiConfig.driverHolder != null) {
      UiConfig.driverHolder ! SubscribeDriverStatusMessage(self)
      UiConfig.driverHolder ! SubscribeDriverDevToolsEventMessage(self)
      UiConfig.driverHolder ! SubscribeCommandLogMessage(self)
    }
  }

  @inline
  private def newEvent(`type`: String, data: Any): ActorEvent = {
    new ActorEvent(`type`, ApiCode.OK, null, data)
  }

}

object WebControllerActor {

  val COMMAND_START = "command.start"
  val COMMAND_LOG = "command.log"
  val DRIVER_STATUS = "driver.status"
  val DRIVER_LOG = "driver.log"

  def props(username: String) = Props(new WebControllerActor(username))

}
