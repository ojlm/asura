package asura.ui.cli

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import asura.common.util.JsonUtils
import asura.ui.cli.actor.DriverPoolActor.PoolOptions
import asura.ui.cli.actor.{AndroidRunnerActor, DriverPoolActor}
import asura.ui.cli.push.PushDataMessage
import asura.ui.cli.push.PushEventListener.MessageType
import asura.ui.cli.runner.AndroidRunner.ConfigParams
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

  var webDriverPoolActor: ActorRef = null
  var androidRunnerActor: ActorRef = null

  def startAndroidRunner(params: ConfigParams): Unit = {
    androidRunnerActor = CliSystem.system.actorOf(AndroidRunnerActor.props(params, ec), "android")
  }

  def startWebDriverPool(options: PoolOptions): Unit = {
    webDriverPoolActor = CliSystem.system.actorOf(DriverPoolActor.props(options), "chrome-pool")
  }

  def getDevices(): Future[Seq[String]] = {
    if (androidRunnerActor != null) {
      (androidRunnerActor ? AndroidRunnerActor.GetDevices).asInstanceOf[Future[Seq[String]]]
        .recover {
          case t: Throwable =>
            logger.error("{}", t)
            Nil
        }
    } else {
      Future.successful(Nil)
    }
  }

  def getTask(id: String): Future[TaskInfo] = {
    if (webDriverPoolActor != null) {
      (webDriverPoolActor ? DriverPoolActor.GetTaskMessage(id)).asInstanceOf[Future[TaskInfo]]
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
    if (webDriverPoolActor != null) {
      webDriverPoolActor ! message
    }
  }

  def sendToPool(message: PushDataMessage): Unit = {
    if (webDriverPoolActor != null) {
      message.`type` match {
        case MessageType.DRIVER_COMMEND_EVENT =>
          if (message.data != null) {
            val task = JsonUtils.mapper.convertValue(message.data, classOf[TaskInfo])
            webDriverPoolActor ! task
          }
        case _ => // ignored
      }
    }
  }

}
