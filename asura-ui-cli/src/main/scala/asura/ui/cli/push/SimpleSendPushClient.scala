package asura.ui.cli.push

import asura.ui.cli.push.PushEventListener.{DriverTaskInfoEvent, MessageType}

trait SimpleSendPushClient extends PushEventListener {

  def send(data: PushDataMessage): Unit

  override def driverPoolEvent(event: PushEventListener.DriverPoolEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_POOL_EVENT, event))
  }

  override def driverStatusEvent(event: PushEventListener.DriverStatusEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_STATUS_EVENT, event))
  }

  override def driverTaskInfoEvent(event: DriverTaskInfoEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_TASK_EVENT, event))
  }

  override def driverDevToolsEvent(event: PushEventListener.DriverDevToolsEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_DEVTOOLS_EVENT, event))
  }

  override def driverCommandLogEvent(event: PushEventListener.DriverCommandLogEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_COMMAND_LOG_EVENT, event))
  }

  override def driverCommandResultEvent(event: PushEventListener.DriverCommandResultEvent): Unit = {
    send(PushDataMessage(MessageType.DRIVER_COMMAND_RESULT_EVENT, event))
  }

}
