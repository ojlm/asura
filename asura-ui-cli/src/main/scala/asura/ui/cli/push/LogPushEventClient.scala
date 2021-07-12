package asura.ui.cli.push

import com.typesafe.scalalogging.Logger

case class LogPushEventClient(options: PushOptions) extends PushEventListener {

  val logger = Logger(getClass.getSimpleName)

  override def driverPoolEvent(event: PushEventListener.DriverPoolEvent): Unit = {
    logger.info(s"DriverPoolEvent{running: ${event.running}, idle: ${event.idle}, core: ${event.core}, max: ${event.max}}")
  }

  override def driverStatusEvent(event: PushEventListener.DriverStatusEvent): Unit = {
    logger.info(s"DriverStatusEvent{port: ${event.driverPort}, status: ${event.status}, screen: ${if (event.screen != null) event.screen.size else 0}}")
  }

  override def driverTaskInfoEvent(event: PushEventListener.DriverTaskInfoEvent): Unit = {
    logger.info(s"DriverTaskInfoEvent{tasks: ${event.tasks.size}}")
  }

  override def driverDevToolsEvent(event: PushEventListener.DriverDevToolsEvent): Unit = {
    val params = event.params
    logger.info(s"DriverDevToolsEvent{method: ${params.method}}")
  }

  override def driverCommandLogEvent(event: PushEventListener.DriverCommandLogEvent): Unit = {
    val log = event.log
    logger.info(s"DriverCommandLogEvent{command: ${log.command}, type: ${log.`type`}}")
  }

  override def driverCommandResultEvent(event: PushEventListener.DriverCommandResultEvent): Unit = {
    logger.info(s"DriverCommandResultEvent:{${event.results}}")
  }

  override def close(): Unit = {
    logger.info(s"closed.")
  }

}
