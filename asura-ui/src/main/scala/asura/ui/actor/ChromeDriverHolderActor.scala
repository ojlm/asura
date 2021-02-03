package asura.ui.actor

import java.util
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, HostUtils, LogUtils}
import asura.ui.UiConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.command.{CommandRunner, Commands, KarateCommandRunner, MonkeyCommandRunner}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver.{DevToolsProtocol, _}
import asura.ui.model.ChromeDriverInfo

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ChromeDriverHolderActor(
                               localChrome: ChromeDriverInfo,
                               uiDriverProvider: UiDriverProvider,
                               syncInterval: Int,
                               taskListener: ActorRef,
                             )(implicit ec: ExecutionContext) extends BaseActor {

  var driver: CustomChromeDriver = null
  val currentStatus = DriverStatus()

  val stopNow = new AtomicBoolean(false)

  val driverStatusEventBus = new DriverStatusEventBus(context.system)
  val driverDevToolsEventBus = new DriverDevToolsEventBus(context.system)
  val driverCommandLogEventBus = new DriverCommandLogEventBus(context.system)

  override def receive: Receive = {
    case NewDriver(driver) =>
      this.driver = driver
    case GetDriver(tpe) =>
      tpe match {
        case Drivers.CHROME => sender() ! driver
        case _ => sender() ! driver
      }
    case SubscribeDriverStatusMessage(ref) =>
      driverStatusEventBus.subscribe(ref, self)
    case DriverStatusMessage(status) =>
      driverStatusEventBus.publish(PublishDriverStatusMessage(self, status))
    case SubscribeDriverDevToolsEventMessage(ref) =>
      driverDevToolsEventBus.subscribe(ref, self)
    case DriverDevToolsMessage(params) =>
      if (needSendDriverLogToTaskListener() && DevToolsProtocol.isNeedLog(params.get(DevToolsProtocol.METHOD))) {
        taskListener ! TaskListenerDriverDevToolsMessage(currentStatus.command.meta, params)
      }
      driverDevToolsEventBus.publish(PublishDriverDevToolsMessage(self, params))
    case SubscribeCommandLogMessage(ref) =>
      driverCommandLogEventBus.subscribe(ref, self)
    case log: DriverCommandLog =>
      if (needSendCommandLogToTaskListener()) {
        taskListener ! TaskListenerDriverCommandLogMessage(currentStatus.command.meta, log)
      }
      driverCommandLogEventBus.publish(PublishCommandLogMessage(self, log))
    case msg: DriverCommandEnd =>
      if (canSendToTaskListener()) {
        currentStatus.command.meta.endAt = System.currentTimeMillis()
        taskListener ! TaskListenerEndMessage(currentStatus.command.meta, msg)
      }
      resetCommandStatus()
      checkNeedToRestore(msg)
    case command: DriverCommand =>
      if (command.`type` == "stop") {
        stopNow.compareAndSet(false, true)
      } else {
        var isOk = false
        var msg: String = null
        if (driver != null) {
          if (Commands.support(command.`type`)) {
            if (currentStatus.status == DriverStatus.STATUS_IDLE) {
              isOk = true
              updateCommandRunningStatus(command)
              runCommand(command) pipeTo self
            } else {
              msg = "The driver is already running"
            }
          } else {
            msg = s"Unsupported command: ${command.`type`}"
          }
        } else {
          msg = "The driver is not ready, please try again later"
          tryRestartServer()
        }
        sender() ! DriverCommandStart(isOk, msg, currentStatus)
      }
  }

  private def needSendCommandLogToTaskListener(): Boolean = {
    canSendToTaskListener() && (if (currentStatus.command.options != null) {
      currentStatus.command.options.saveCommandLog
    } else {
      false
    })
  }

  private def needSendDriverLogToTaskListener(): Boolean = {
    canSendToTaskListener() && (if (currentStatus.command.options != null) {
      currentStatus.command.options.saveDriverLog
    } else {
      false
    })
  }

  private def canSendToTaskListener(): Boolean = {
    taskListener != null && currentStatus.command != null &&
      currentStatus.command.meta != null && currentStatus.command.meta.reportId != null
  }

  private def checkNeedToRestore(msg: DriverCommandEnd): Unit = {
    if (msg.msg != null) {
      if (msg.msg.contains("failed to get reply for")) {
        tryRestartServer()
      }
    }
  }

  private def runCommand(command: DriverCommand): Future[DriverCommandEnd] = {
    val run = () => Future {
      val runner: CommandRunner = command.`type` match {
        case Commands.MONKEY => MonkeyCommandRunner(driver, command, stopNow, self)
        case Commands.KARATE => KarateCommandRunner(command, stopNow, self)
      }
      runner.run()
    }
    val result = if (taskListener != null) {
      // create a ui task and return 'reportId'
      command.meta.startAt = System.currentTimeMillis()
      (taskListener ? TaskListenerCreateMessage(command)).flatMap(res => {
        val response = res.asInstanceOf[TaskListenerCreateMessageResponse]
        command.meta.reportId = response.reportId
        command.meta.day = response.day
        run()
      })
    } else {
      run()
    }
    result.recover {
      case t: Throwable =>
        log.warning("{}", LogUtils.stackTraceToString(t))
        DriverCommandEnd(command.`type`, false, t.getMessage)
    }
  }

  def resetCommandStatus(): Unit = {
    currentStatus.startAt = null
    currentStatus.updateAt = DateUtils.nowDateTime
    currentStatus.status = DriverStatus.STATUS_IDLE
    currentStatus.command = null
  }

  def updateCommandRunningStatus(command: DriverCommand): Unit = {
    val time = DateUtils.nowDateTime
    currentStatus.startAt = time
    currentStatus.updateAt = time
    currentStatus.status = DriverStatus.STATUS_RUNNING
    currentStatus.command = command
  }

  private def tryRestartServer(): Unit = {
    Future {
      val driver = CustomChromeDriver.start(false, params => {
        self ! DriverDevToolsMessage(params)
      })
      NewDriver(driver)
    }.recover({
      case t: Throwable =>
        log.error("{}", t)
        NewDriver(null)
    }).pipeTo(self)
  }

  override def preStart(): Unit = {
    context.system.scheduler.scheduleAtFixedRate(1 seconds, 5 seconds)(() => {
      currentStatus.updateAt = DateUtils.nowDateTime
      self ! DriverStatusMessage(currentStatus)
    })
    if (localChrome != null) {
      localChrome.hostname = HostUtils.hostname
      context.system.scheduler.scheduleAtFixedRate(1 seconds, syncInterval seconds)(() => {
        if (driver != null) {
          Future {
            localChrome.timestamp = System.currentTimeMillis()
            localChrome.screenCapture = Base64.getEncoder.encodeToString(driver.screenshot(true))
            uiDriverProvider.register(Drivers.CHROME, localChrome)
          }
        }
      })
    }
    tryRestartServer()
  }

}

object ChromeDriverHolderActor {

  def props(
             localChrome: ChromeDriverInfo,
             uiDriverProvider: UiDriverProvider,
             taskListener: ActorRef,
             syncInterval: Int,
             ec: ExecutionContext = ExecutionContext.global
           ) = Props(new ChromeDriverHolderActor(localChrome, uiDriverProvider, syncInterval, taskListener)(ec))

  case class NewDriver(driver: CustomChromeDriver)

  case class GetDriver(tpe: String)

  case class SubscribeDriverDevToolsEventMessage(ref: ActorRef)

  case class SubscribeDriverStatusMessage(ref: ActorRef)

  case class SubscribeCommandLogMessage(ref: ActorRef)

  case class DriverDevToolsMessage(params: util.Map[String, AnyRef])

  case class DriverStatusMessage(status: DriverStatus)

  case class TaskListenerCreateMessage(command: DriverCommand)

  case class TaskListenerCreateMessageResponse(reportId: String, day: String)

  case class TaskListenerEndMessage(meta: CommandMeta, data: DriverCommandEnd)

  case class TaskListenerDriverDevToolsMessage(meta: CommandMeta, data: util.Map[String, AnyRef])

  case class TaskListenerDriverCommandLogMessage(meta: CommandMeta, data: DriverCommandLog)

}
