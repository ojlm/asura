package asura.ui.cli.push

import com.typesafe.scalalogging.Logger

case class LogPushEventClient(options: PushOptions) extends PushEventListener {

  val logger = Logger(getClass)

  override def driverPoolEvent(event: PushEventListener.DriverPoolEvent): Unit = {
    logger.info(s"DriverPoolEvent{running: ${event.running}, idle: ${event.idle}, core: ${event.core}, max: ${event.core}}")
  }

  override def driverStatusEvent(event: PushEventListener.DriverStatusEvent): Unit = {
    logger.info(s"DriverStatusEvent{port: ${event.driverPort}, status: ${event.status}}")
  }

  override def driverDevToolsEvent(event: PushEventListener.DriverDevToolsEvent): Unit = {
    logger.info(s"DriverDevToolsEvent{}")
  }

  override def driverCommandLogEvent(event: PushEventListener.DriverCommandLogEvent): Unit = {
    logger.info(s"DriverDevToolsEvent{}")
  }

  override def driverCommandResultEvent(event: PushEventListener.DriverCommandResultEvent): Unit = {
    logger.info(s"DriverDevToolsEvent{}")
  }

  override def close(): Unit = {
    logger.info(s"closed.")
  }

}
