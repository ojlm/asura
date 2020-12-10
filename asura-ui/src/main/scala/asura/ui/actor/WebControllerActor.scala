package asura.ui.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{ActorEvent, BaseActor, SenderMessage}
import asura.common.model.ApiCode
import asura.ui.UiConfig
import asura.ui.actor.ChromeDriverHolderActor.{SubscribeDriverDevToolsEventMessage, SubscribeDriverStatusMessage}
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver.{DriverCommand, DriverCommandResult, DriverStatus}
import com.intuit.karate.driver.DevToolsMessage

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
    case result: DriverCommandResult =>
      wsActor ! commandEvent(result)
    case PublishDriverStatusMessage(_, status) =>
      wsActor ! statusEvent(status)
    case PublishDriverDevToolsMessage(_, msg) =>
      wsActor ! driverEvent(msg)
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
    }
  }

  @inline
  private def commandEvent(result: DriverCommandResult): ActorEvent = {
    new ActorEvent("command", ApiCode.OK, null, result)
  }

  @inline
  private def statusEvent(status: DriverStatus): ActorEvent = {
    new ActorEvent("status", ApiCode.OK, null, status)
  }

  @inline
  private def driverEvent(msg: DevToolsMessage): ActorEvent = {
    new ActorEvent("driver", ApiCode.OK, null, msg)
  }

}

object WebControllerActor {

  def props(username: String) = Props(new WebControllerActor(username))

}
