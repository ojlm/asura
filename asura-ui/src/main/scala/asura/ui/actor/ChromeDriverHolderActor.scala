package asura.ui.actor

import java.util.Date

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.DateUtils
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage
import asura.ui.driver._
import com.intuit.karate.driver.DevToolsMessage

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ChromeDriverHolderActor()(implicit ec: ExecutionContext) extends BaseActor {

  var driver: CustomChromeDriver = null
  val currentStatus = DriverStatus()

  val driverStatusEventBus = new DriverStatusEventBus(context.system)
  val driverDevToolsEventBus = new DriverDevToolsEventBus(context.system)

  override def receive: Receive = {
    case NewDriver(driver) =>
      this.driver = driver
    case SubscribeDriverStatusMessage(ref) =>
      driverStatusEventBus.subscribe(ref, self)
    case DriverStatusMessage(status) =>
      driverStatusEventBus.publish(PublishDriverStatusMessage(self, status))
    case SubscribeDriverDevToolsEventMessage(ref) =>
      driverDevToolsEventBus.subscribe(ref, self)
    case DriverDevToolsMessage(msg) =>
      driverDevToolsEventBus.publish(PublishDriverDevToolsMessage(self, msg))
    case command: DriverCommand =>
      var isOk = false
      if (currentStatus.status == DriverStatus.STATUS_IDLE) {
        isOk = true
        updateCommandStatus(command)
        runCommand(command)
      }
      sender() ! DriverCommandResult(isOk, currentStatus)
  }

  def runCommand(command: DriverCommand): Unit = {
    // TODO: run in async
    command.`type` match {
      case _ =>
    }
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
        val driver = CustomChromeDriver.start(false, msg => {
          self ! DriverDevToolsMessage(msg)
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

  case class DriverDevToolsMessage(msg: DevToolsMessage)

  case class DriverStatusMessage(status: DriverStatus)

}
