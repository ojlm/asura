package asura.ui.cli.actor

import java.util.function.Consumer

import scala.concurrent.{Future, Promise}

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.DriverPoolItemActor._
import asura.ui.cli.push.PushEventListener
import asura.ui.cli.push.PushEventListener.DriverDevToolsEvent
import asura.ui.cli.task.{TaskDevToolParams, TaskInfo}
import asura.ui.model.RemoteHost
import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.driver.chrome.Chrome
import com.intuit.karate.driver.indigo.IndigoDriver
import com.intuit.karate.driver.{Driver, DriverOptions}

class RemoteDriverActor(
                         options: java.util.Map[String, AnyRef],
                         sr: ScenarioRuntime,
                         remote: RemoteHost,
                         listener: PushEventListener,
                       ) extends BaseActor {

  val driverPromise = Promise[Driver]()
  var driver: Driver = null
  var task: TaskInfo = null

  override def receive: Receive = {
    case GetDriverMessage =>
      val actor = sender()
      driverPromise.future.foreach(driver => actor ! driver)(context.dispatcher)
    case TaskInfoMessage(task) =>
      this.task = task
    case TaskOverMessage(_) =>
      this.task = null
    case msg =>
      log.info(s"Unknown type: ${msg.getClass.getName}.")
  }

  override def preStart(): Unit = {
    startDriver()
  }

  override def postStop(): Unit = {
    if (!driverPromise.isCompleted) driverPromise.failure(new RuntimeException("stopped"))
    if (driver != null) {
      driver.quit()
    }
  }

  private def startDriver(): Unit = {
    implicit val ec = CliSystem.ec
    Future {
      options.put("host", remote.host)
      options.put("port", Int.box(remote.port))
      driver = options.getOrDefault("type", "chrome") match {
        case "chrome" | "electron" =>
          val filter: Consumer[java.util.Map[String, AnyRef]] = {
            if (listener != null && listener.options.pushLogs) {
              params: java.util.Map[String, AnyRef] => {
                if (task != null && task.meta != null && params.containsKey("method")) {
                  listener.driverDevToolsEvent(DriverDevToolsEvent(task.meta, TaskDevToolParams(params)))
                }
              }
            } else {
              null
            }
          }
          Chrome.start(options, filter, false)
        case "indigo" =>
          IndigoDriver.start(options, sr)
        case _ =>
          DriverOptions.start(options, sr)
      }
      driverPromise.success(driver)
    }.recover {
      case t: Throwable =>
        driverPromise.failure(t)
        log.error(LogUtils.stackTraceToString(t))
        context stop self
    }
  }

}

object RemoteDriverActor {
  def props(
             options: java.util.Map[String, AnyRef],
             sr: ScenarioRuntime,
             remote: RemoteHost,
             listener: PushEventListener,
           ) = {
    Props(new RemoteDriverActor(options, sr, remote, listener))
  }
}
