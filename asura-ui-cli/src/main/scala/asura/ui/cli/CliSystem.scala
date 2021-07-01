package asura.ui.cli

import scala.concurrent.ExecutionContext

import akka.actor.{ActorRef, ActorSystem}
import asura.common.util.JsonUtils
import asura.ui.cli.actor.DriverPoolActor
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.push.PushDataMessage
import asura.ui.cli.push.PushEventListener.MessageType
import asura.ui.cli.task.TaskInfo

object CliSystem {

  val LOG_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"

  lazy implicit val ec = ExecutionContext.global

  lazy val system: ActorSystem = {
    ActorSystem("ui-cli")
  }

  var driverPoolActor: ActorRef = null

  def startWebDriverPool(options: PoolOptions): Unit = {
    driverPoolActor = CliSystem.system.actorOf(DriverPoolActor.props(options), "chrome-pool")
  }

  def sendToPool(message: TaskInfo): Unit = {
    if (driverPoolActor != null) {
      driverPoolActor ! message
    }
  }

  def sendToPool(message: PushDataMessage): Unit = {
    if (driverPoolActor != null) {
      message.`type` match {
        case MessageType.DRIVER_COMMEND_EVENT =>
          if (message.data != null) {
            val task = JsonUtils.mapper.convertValue(message.data, classOf[TaskInfo])
            driverPoolActor ! task
          }
        case _ => // ignored
      }
    }
  }

}
