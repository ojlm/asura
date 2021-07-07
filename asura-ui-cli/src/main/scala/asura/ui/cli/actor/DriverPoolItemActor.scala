package asura.ui.cli.actor

import java.io.File
import java.util.function.Consumer

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

import akka.actor.{Cancellable, Props}
import asura.common.actor.BaseActor
import asura.common.util.FutureUtils.RichFuture
import asura.common.util.LogUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.actor.DriverPoolItemActor._
import asura.ui.cli.push.PushEventListener
import asura.ui.cli.push.PushEventListener.{DriverDevToolsEvent, DriverStatusEvent, STATUS_IDLE, STATUS_RUNNING}
import asura.ui.cli.server.ServerProxyConfig.PortSelector
import asura.ui.cli.task.{TaskDevToolParams, TaskDriver, TaskInfo}
import asura.ui.model.ChromeTargetPage
import asura.ui.util.ChromeDevTools
import com.intuit.karate.FileUtils
import com.intuit.karate.driver.chrome.Chrome

class DriverPoolItemActor(
                           options: java.util.HashMap[String, Object],
                           selector: PortSelector,
                           listener: PushEventListener,
                         ) extends BaseActor {

  var statusScheduler: Cancellable = null
  var screenScheduler: Cancellable = null
  val pageIds = mutable.Set[String]()
  val status: DriverStatusEvent = if (listener != null) {
    DriverStatusEvent(listener.options.pushIp, listener.options.pushPort)
  } else {
    DriverStatusEvent("localhost", 0)
  }
  val driverPromise = Promise[Chrome]()
  var driver: Chrome = null

  override def receive: Receive = {
    case GetDriverMessage =>
      val actor = sender()
      driverPromise.future.foreach(driver => actor ! driver)(context.dispatcher)
    case TaskInfoMessage(task) =>
      status.status = STATUS_RUNNING
      status.task = task
    case TaskOverMessage =>
      status.status = STATUS_IDLE
      status.task = null
    case PagesMessage(pages) =>
      pages.foreach(page => {
        if (!pageIds.contains(page.id)) {
          pageIds += page.id
          selector.set(page.id, status.driverPort)
        }
      })
    case msg =>
      log.info(s"Unknown type: ${msg.getClass.getName}.")
  }

  override def preStart(): Unit = {
    startDriver()
  }

  override def postStop(): Unit = {
    if (statusScheduler != null) statusScheduler.cancel()
    if (screenScheduler != null) screenScheduler.cancel()
    if (!driverPromise.isCompleted) driverPromise.failure(new RuntimeException("stopped"))
    pageIds.foreach(id => selector.remove(id))
    if (driver != null) {
      if (options.getOrDefault("removeUserDataDir", Boolean.box(true)).asInstanceOf[Boolean]) {
        FileUtils.deleteDirectory(new File(driver.getOptions.userDataDir))
      }
      driver.quit()
    }
  }

  private def startDriver(): Unit = {
    implicit val ec = CliSystem.ec
    Future {
      val filter: Consumer[java.util.Map[String, AnyRef]] = {
        if (listener != null && listener.options.pushLogs) {
          params: java.util.Map[String, AnyRef] => {
            if (status.task != null && status.task.meta != null) {
              listener.driverDevToolsEvent(DriverDevToolsEvent(status.task.meta, TaskDevToolParams(params)))
            }
          }
        } else {
          null
        }
      }
      driver = Chrome.start(options, filter, false)
      status.driverPort = driver.getOptions.port
    }.flatMap(_ => {
      ChromeDevTools.getVersion("127.0.0.1", status.driverPort)(CliSystem.ec)
    }).map(version => {
      driverPromise.success(driver)
      status.version = version
      startPushDriverStatus()
    }).recover {
      case t: Throwable =>
        driverPromise.failure(t)
        log.error(LogUtils.stackTraceToString(t))
        context stop self
    }
  }

  private def getScreenShot(): Future[String] = {
    implicit val ec = CliSystem.ec
    Future {
      driver.screenshotAsBase64()
    }.recover {
      case _: Throwable => null
    }
  }

  private def startPushDriverStatus(): Unit = {
    if (listener != null && listener.options.pushStatus) {
      val pushOptions = listener.options
      implicit val ec = CliSystem.ec
      statusScheduler = context.system.scheduler.scheduleWithFixedDelay(0 seconds, pushOptions.pushInterval seconds)(() => {
        ChromeDevTools.getTargetPages("127.0.0.1", status.driverPort)
          .map(targets => {
            if (status.task != null) {
              status.task.targets += (driver -> TaskDriver(status.host, status.port, status.driverPort, targets))
            }
            self ! PagesMessage(targets)
            status.targets = targets
            status.timestamp = System.currentTimeMillis()
            listener.driverStatusEvent(status)
          })
          .recover {
            case t: Throwable =>
              log.error(LogUtils.stackTraceToString(t))
              context stop self
          }.await
      })
      if (listener.options.pushScreen) {
        screenScheduler = context.system.scheduler.scheduleWithFixedDelay(0 seconds, pushOptions.pushInterval * 5 seconds)(() => {
          getScreenShot().map(screen => {
            status.screen = screen
          })
        })
      }
    }
  }

}

object DriverPoolItemActor {

  def props(
             options: java.util.HashMap[String, Object],
             selector: PortSelector,
             listener: PushEventListener,
           ) = {
    Props(new DriverPoolItemActor(options, selector, listener))
  }

  case object GetDriverMessage

  case class TaskInfoMessage(task: TaskInfo)

  case object TaskOverMessage

  case class PagesMessage(pages: Seq[ChromeTargetPage])

}
