package asura.ui.actor

import java.util
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, JsonUtils, LogUtils}
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.command.MonkeyCommand.MonkeyCommandParams
import asura.ui.command.{Commands, MonkeyCommand}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ChromeDriverHolderActor()(implicit ec: ExecutionContext) extends BaseActor {

  var driver: CustomChromeDriver = null
  val currentStatus = DriverStatus()

  val stopNow = new AtomicBoolean(false)

  val driverStatusEventBus = new DriverStatusEventBus(context.system)
  val driverDevToolsEventBus = new DriverDevToolsEventBus(context.system)
  val driverCommandLogEventBus = new DriverCommandLogEventBus(context.system)

  override def receive: Receive = {
    case NewDriver(driver) =>
      this.driver = driver
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
    case DriverCommandEnd(_, _, _) =>
      resetCommandStatus()
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
        }
        sender() ! DriverCommandStart(isOk, msg, currentStatus)
      }
  }

  private def runCommand(command: DriverCommand): Future[DriverCommandEnd] = {
    command.`type` match {
      case Commands.MONKEY => runMonkey(command)
    }
  }

  private def runMonkey(command: DriverCommand): Future[DriverCommandEnd] = {
    Future {
      val params = JsonUtils.mapper.convertValue(command.params, classOf[MonkeyCommandParams])
      params.validate()
      if (params.generateCount == 0 && params.maxDuration == 0) {
        DriverCommandEnd(true)
      } else {
        val monkey = MonkeyCommand(driver, params, log => self ! log)
        monkey.init()
        val start = System.currentTimeMillis()
        val durationInMs = params.maxDuration * 1000
        val checkDuration = () => {
          if (stopNow.get()) {
            stopNow.set(false)
            false
          } else {
            if (durationInMs > 0) {
              ((System.currentTimeMillis() - start) < durationInMs)
            } else {
              true
            }
          }
        }
        if (params.generateCount > 0) {
          var i = 0
          while (i < params.generateCount && checkDuration()) {
            monkey.generate()
            Thread.sleep(params.delta) // ugly
            i += 1
          }
        } else {
          while (checkDuration()) {
            monkey.generate()
            Thread.sleep(params.delta) // ugly
          }
        }
        DriverCommandEnd(true)
      }
    }.recover {
      case t: Throwable =>
        log.warning("{}", LogUtils.stackTraceToString(t))
        DriverCommandEnd(false, t.getMessage)
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

  override def preStart(): Unit = {
    context.system.scheduler.scheduleAtFixedRate(1 seconds, 5 seconds)(() => {
      currentStatus.updateAt = DateUtils.nowDateTime
      self ! DriverStatusMessage(currentStatus)
    })
    Future {
      try {
        val driver = CustomChromeDriver.start(false, params => {
          self ! DriverDevToolsMessage(params)
        })
        NewDriver(driver)
      } catch {
        case t: Throwable =>
          log.error("{}", t)
          NewDriver(null)
      }
    }.pipeTo(self)
  }

}

object ChromeDriverHolderActor {

  def props(ec: ExecutionContext = ExecutionContext.global) = Props(new ChromeDriverHolderActor()(ec))

  case class NewDriver(driver: CustomChromeDriver)

  case class SubscribeDriverDevToolsEventMessage(ref: ActorRef)

  case class SubscribeDriverStatusMessage(ref: ActorRef)

  case class SubscribeCommandLogMessage(ref: ActorRef)

  case class DriverDevToolsMessage(params: util.Map[String, AnyRef])

  case class DriverStatusMessage(status: DriverStatus)

}
