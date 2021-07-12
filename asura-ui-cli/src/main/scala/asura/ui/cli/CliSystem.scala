package asura.ui.cli

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import asura.common.util.JsonUtils
import asura.ui.cli.actor.DriverPoolActor
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.push.PushDataMessage
import asura.ui.cli.push.PushEventListener.MessageType
import asura.ui.cli.task.TaskInfo
import com.typesafe.scalalogging.Logger

object CliSystem {

  val LOG_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"
  val logger = Logger("CliSystem")

  lazy implicit val ec = ExecutionContext.global
  implicit val ACTOR_ASK_TIMEOUT: Timeout = 10 seconds

  lazy val system: ActorSystem = {
    ActorSystem("ui-cli")
  }

  var driverPoolActor: ActorRef = null

  def startWebDriverPool(options: PoolOptions): Unit = {
    driverPoolActor = CliSystem.system.actorOf(DriverPoolActor.props(options), "chrome-pool")
  }

  def getTask(id: String): Future[TaskInfo] = {
    if (driverPoolActor != null) {
      (driverPoolActor ? DriverPoolActor.GetTaskMessage(id)).asInstanceOf[Future[TaskInfo]]
        .recover {
          case t: Throwable =>
            logger.error("{}", t)
            TaskInfo.EMPTY
        }
    } else {
      Future.successful(TaskInfo.EMPTY)
    }
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
