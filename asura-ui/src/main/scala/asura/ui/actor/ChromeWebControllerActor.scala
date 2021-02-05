package asura.ui.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{ActorEvent, BaseActor, SenderMessage}
import asura.common.model.ApiCode
import asura.ui.UiConfig
import asura.ui.actor.ChromeDriverHolderActor.{SubscribeCommandLogMessage, SubscribeDriverDevToolsEventMessage, SubscribeDriverStatusMessage}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver.{CommandMeta, DriverCommand, DriverCommandStart}

class ChromeWebControllerActor(group: String, project: String, taskId: String, username: String) extends BaseActor {

  // represent websocket connection
  var wsActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      wsActor = wsSender
      subscribeDriverEvent()
    case command: DriverCommand =>
      command.meta = CommandMeta(group, project, taskId, username)
      sendCommand(command)
    case msg: DriverCommandStart =>
      wsActor ! newEvent(ChromeWebControllerActor.COMMAND_START, msg)
    case PublishCommandLogMessage(_, log) =>
      wsActor ! newEvent(ChromeWebControllerActor.COMMAND_LOG, log)
    case PublishDriverStatusMessage(_, status) =>
      wsActor ! newEvent(ChromeWebControllerActor.DRIVER_STATUS, status)
    case PublishDriverDevToolsMessage(_, msg) =>
      wsActor ! newEvent(ChromeWebControllerActor.DRIVER_LOG, msg)
  }

  private def sendCommand(command: DriverCommand): Unit = {
    if (UiConfig.localChromeDriver != null) {
      UiConfig.localChromeDriver ! command
    }
  }

  private def subscribeDriverEvent(): Unit = {
    if (UiConfig.localChromeDriver != null) {
      UiConfig.localChromeDriver ! SubscribeDriverStatusMessage(self)
      UiConfig.localChromeDriver ! SubscribeDriverDevToolsEventMessage(self)
      UiConfig.localChromeDriver ! SubscribeCommandLogMessage(self)
    }
  }

  @inline
  private def newEvent(`type`: String, data: Any): ActorEvent = {
    new ActorEvent(`type`, ApiCode.OK, null, data)
  }

}

object ChromeWebControllerActor {

  val COMMAND_START = "command.start"
  val COMMAND_LOG = "command.log"
  val DRIVER_STATUS = "driver.status"
  val DRIVER_LOG = "driver.log"

  def props(
             group: String,
             project: String,
             taskId: String,
             username: String,
           ) = Props(new ChromeWebControllerActor(group, project, taskId, username))

}
