package asura.ui.actor

import java.util
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, LogUtils}
import asura.ui.UiConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.command.{CommandRunner, Commands, WebMonkeyCommandRunner}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver.{DevToolsProtocol, _}
import asura.ui.model.{ChromeDriverInfo, ChromeVersion, ServoAddress}
import asura.ui.util.ChromeDevTools

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ChromeDriverHolderActor(
                               driverInfo: ChromeDriverInfo, // register to provider
                               uiDriverProvider: UiDriverProvider,
                               syncInterval: Int,
                               taskListener: ActorRef,
                               options: util.HashMap[String, Object],
                             )(implicit ec: ExecutionContext) extends BaseActor {

  val servoAddr: ServoAddress = if (driverInfo != null) ServoAddress(driverInfo.host, driverInfo.port, driverInfo.hostname) else null
  var driver: CustomChromeDriver = null
  var version: ChromeVersion = null
  val currentStatus = DriverStatus()

  val stopNow = new AtomicBoolean(false)

  val driverStatusEventBus = new DriverStatusEventBus(context.system)
  val driverDevToolsEventBus = new DriverDevToolsEventBus(context.system)
  val driverCommandLogEventBus = new DriverCommandLogEventBus(context.system)

  override def receive: Receive = {
    case NewDriver(driver, version) =>
      this.driver = driver
      this.version = version
    case SubscribeDriverStatusMessage(ref) =>
      driverStatusEventBus.subscribe(ref, self)
    case DriverStatusMessage(status) =>
      driverStatusEventBus.publish(PublishDriverStatusMessage(self, status))
    case SubscribeDriverDevToolsEventMessage(ref) =>
      driverDevToolsEventBus.subscribe(ref, self)
    case log: DriverDevToolsMessage =>
      if (needSendDriverLogToTaskListener() && DevToolsProtocol.isNeedLog(log.params.get(DevToolsProtocol.METHOD))) {
        taskListener ! TaskListenerDriverDevToolsMessage(log)
      }
      driverDevToolsEventBus.publish(PublishDriverDevToolsMessage(self, log))
    case SubscribeCommandLogMessage(ref) =>
      driverCommandLogEventBus.subscribe(ref, self)
    case log: DriverCommandLog =>
      if (needSendCommandLogToTaskListener()) {
        taskListener ! TaskListenerDriverCommandLogMessage(log)
      }
      driverCommandLogEventBus.publish(PublishCommandLogMessage(self, log))
    case msg: DriverCommandEnd =>
      if (canSendToTaskListener()) {
        currentStatus.command.meta.endAt = System.currentTimeMillis()
        taskListener ! TaskListenerEndMessage(currentStatus.command.meta, msg)
      }
      resetCommandStatus()
      checkNeedToRestore(msg.msg)
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

  private def isTimeoutError(msg: String): Boolean = {
    msg.contains("failed to get reply for")
  }

  private def checkNeedToRestore(errorMsg: String): Unit = {
    if (errorMsg != null) {
      val startNew = options != null && options.getOrDefault("start", Boolean.box(false)).asInstanceOf[Boolean]
      if (isTimeoutError(errorMsg) && !startNew) {
        tryRestartServer()
      }
    }
  }

  private def runCommand(command: DriverCommand): Future[DriverCommandEnd] = {
    val run = () => Future {
      val runner: CommandRunner = command.`type` match {
        case Commands.WEB_MONKEY => WebMonkeyCommandRunner(driver, command.meta, command, stopNow, self)
        case Commands.KARATE => throw new RuntimeException("TBD")
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
        log.warning(LogUtils.stackTraceToString(t))
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
      val sendFunc = (params: util.Map[String, AnyRef]) => {
        if (currentStatus.command != null && currentStatus.command.meta != null) {
          self ! DriverDevToolsMessage(currentStatus.command.meta, params)
        }
      }
      if (options == null) {
        CustomChromeDriver.start(false, sendFunc)
      } else {
        CustomChromeDriver.start(options, sendFunc)
      }
    }.flatMap(driver => {
      if (driverInfo != null) {
        ChromeDevTools.getVersion(driverInfo).map(version => NewDriver(driver, version))
      } else {
        Future.successful(NewDriver(driver, null))
      }
    }).recover({
      case t: Throwable =>
        log.error(LogUtils.stackTraceToString(t))
        NewDriver(null, null)
    }).pipeTo(self)
  }

  override def preStart(): Unit = {
    tryRestartServer()
    context.system.scheduler.scheduleAtFixedRate(30 seconds, syncInterval seconds)(() => {
      if (driver != null) {
        currentStatus.updateAt = DateUtils.nowDateTime
        self ! DriverStatusMessage(currentStatus)
        if (driverInfo != null) { // push to the register
          ChromeDevTools.getTargetPages(driverInfo).map(targets => {
            driverInfo.timestamp = System.currentTimeMillis()
            driverInfo.screenCapture = driver.screenshotAsBase64()
            driverInfo.status = currentStatus
            driverInfo.targets = targets
            driverInfo.version = version
            uiDriverProvider.register(Drivers.CHROME, driverInfo)
          }).recover({
            case t: Throwable => log.error(LogUtils.stackTraceToString(t))
          })
        }
      } else {
        tryRestartServer()
      }
    })
  }

}

object ChromeDriverHolderActor {

  def props(
             driverInfo: ChromeDriverInfo,
             uiDriverProvider: UiDriverProvider,
             taskListener: ActorRef,
             syncInterval: Int,
             options: util.HashMap[String, Object],
             ec: ExecutionContext = ExecutionContext.global
           ) = Props(new ChromeDriverHolderActor(driverInfo, uiDriverProvider, syncInterval, taskListener, options)(ec))

  case class NewDriver(driver: CustomChromeDriver, version: ChromeVersion)

  case class SubscribeDriverDevToolsEventMessage(ref: ActorRef)

  case class SubscribeDriverStatusMessage(ref: ActorRef)

  case class SubscribeCommandLogMessage(ref: ActorRef)

  case class DriverDevToolsMessage(meta: CommandMeta, params: util.Map[String, AnyRef])

  case class DriverStatusMessage(status: DriverStatus)

  case class TaskListenerCreateMessage(command: DriverCommand)

  case class TaskListenerCreateMessageResponse(reportId: String, day: String)

  case class TaskListenerEndMessage(meta: CommandMeta, data: DriverCommandEnd)

  case class TaskListenerDriverDevToolsMessage(data: DriverDevToolsMessage)

  case class TaskListenerDriverCommandLogMessage(data: DriverCommandLog)

}
