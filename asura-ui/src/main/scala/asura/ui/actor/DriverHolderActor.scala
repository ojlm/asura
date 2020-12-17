package asura.ui.actor

import java.util
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, LogUtils}
import asura.ui.actor.DriverHolderActor._
import asura.ui.command.{CommandRunner, Commands, KarateCommandRunner, MonkeyCommandRunner}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

// for now just the chrome
class DriverHolderActor()(implicit ec: ExecutionContext) extends BaseActor {

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
      driverDevToolsEventBus.publish(PublishDriverDevToolsMessage(self, params))
    case SubscribeCommandLogMessage(ref) =>
      driverCommandLogEventBus.subscribe(ref, self)
    case log: DriverCommandLog =>
      driverCommandLogEventBus.publish(PublishCommandLogMessage(self, log))
    case msg: DriverCommandEnd =>
      resetCommandStatus()
      dealResult(msg)
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
              updateCommandStatus(command)
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

  private def dealResult(msg: DriverCommandEnd): Unit = {
    // TODO: result
    if (msg.msg != null) {
      if (msg.msg.contains("failed to get reply for")) {
        tryRestartServer()
      }
    }
  }

  private def runCommand(command: DriverCommand): Future[DriverCommandEnd] = {
    Future {
      val runner: CommandRunner = command.`type` match {
        case Commands.MONKEY => MonkeyCommandRunner(driver, command, stopNow, self)
        case Commands.KARATE => KarateCommandRunner(command, stopNow, self)
      }
      runner.run()
    }.recover {
      case t: Throwable =>
        log.warning("{}", LogUtils.stackTraceToString(t))
        DriverCommandEnd(command.`type`, false, t.getMessage)
    }
  }

  def resetCommandStatus(): Unit = {
    currentStatus.updateAt = DateUtils.nowDateTime
    currentStatus.status = DriverStatus.STATUS_IDLE
    currentStatus.command = null
    currentStatus.commandStartAt = 0
  }

  def updateCommandStatus(command: DriverCommand): Unit = {
    currentStatus.updateAt = DateUtils.nowDateTime
    currentStatus.status = DriverStatus.STATUS_RUNNING
    currentStatus.command = command
    currentStatus.commandStartAt = new Date().getTime()
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
    tryRestartServer()
  }

}

object DriverHolderActor {

  def props(ec: ExecutionContext = ExecutionContext.global) = Props(new DriverHolderActor()(ec))

  case class NewDriver(driver: CustomChromeDriver)

  case class GetDriver(tpe: String)

  case class SubscribeDriverDevToolsEventMessage(ref: ActorRef)

  case class SubscribeDriverStatusMessage(ref: ActorRef)

  case class SubscribeCommandLogMessage(ref: ActorRef)

  case class DriverDevToolsMessage(params: util.Map[String, AnyRef])

  case class DriverStatusMessage(status: DriverStatus)

}
