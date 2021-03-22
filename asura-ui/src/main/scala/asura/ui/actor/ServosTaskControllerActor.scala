package asura.ui.actor

import java.util
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{JsonUtils, LogUtils}
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.actor.ServosTaskControllerActor.{RunnerResult, Start}
import asura.ui.command.WebMonkeyCommandRunner.MonkeyCommandParams
import asura.ui.command.{Commands, WebMonkeyCommandRunner}
import asura.ui.driver.{CustomChromeDriver, DriverCommand, DriverCommandEnd, DriverCommandLog}
import asura.ui.model.DriverInitResponse
import asura.ui.model.DriverInitResponse.ServoInitResponseItem

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ServosTaskControllerActor(
                                 command: DriverCommand,
                                 taskListener: ActorRef
                               )(implicit ec: ExecutionContext) extends BaseActor {

  val stopNow = new AtomicBoolean(false)
  var servos: Seq[ServoInitResponseItem] = Nil
  val resultMap = mutable.Map[String, DriverCommandEnd]()
  var resultOk = true

  override def receive: Receive = {
    case Start =>
      val saveDriverLog = if (command.options != null && taskListener != null) command.options.saveDriverLog else false
      val saveCommandLog = if (command.options != null && taskListener != null) command.options.saveCommandLog else false
      val res = ServosTaskControllerActor.initDrivers(command, stopNow, self, saveDriverLog, saveCommandLog)
      res pipeTo sender()
      res pipeTo self
    case response: DriverInitResponse =>
      if (response.isAllOk() && taskListener != null) {
        servos = response.servos
        command.meta.startAt = System.currentTimeMillis()
        taskListener ! TaskListenerCreateMessage(command)
      } else {
        context.stop(self)
      }
    case TaskListenerCreateMessageResponse(reportId, day) =>
      command.meta.reportId = reportId
      command.meta.day = day
      servos.foreach(servo => {
        servo.runner.meta.startAt = command.meta.startAt
        servo.runner.meta.reportId = reportId
        servo.runner.meta.day = day
        run(servo)
      })
    case log: DriverDevToolsMessage =>
      if (log.meta != null && log.meta.reportId != null) {
        taskListener ! TaskListenerDriverDevToolsMessage(log)
      }
    case log: DriverCommandLog =>
      if (log.meta != null && log.meta.reportId != null) {
        taskListener ! TaskListenerDriverCommandLogMessage(log)
      }
    case RunnerResult(item, result) =>
      resultMap += (item.servo.toKey -> result)
      if (!result.ok) resultOk = false
      if (resultMap.size == servos.size) { // all end
        command.meta.endAt = System.currentTimeMillis()
        if (taskListener != null) {
          taskListener ! TaskListenerEndMessage(command.meta, DriverCommandEnd(command.`type`, resultOk, null, resultMap))
        }
        context.stop(self)
      }
    case msg =>
      log.error(s"Unknown type: ${msg.getClass}")
      context.stop(self)
  }

  def run(item: ServoInitResponseItem): Future[RunnerResult] = {
    val result = Future {
      RunnerResult(item, item.runner.run())
    } recover {
      case t: Throwable =>
        val error = LogUtils.stackTraceToString(t)
        log.warning(error)
        RunnerResult(item, DriverCommandEnd(command.`type`, false, error))
    }
    result pipeTo self
  }
}

object ServosTaskControllerActor {

  def props(
             task: DriverCommand,
             taskListener: ActorRef,
             ec: ExecutionContext = ExecutionContext.global,
           ) = {
    Props(new ServosTaskControllerActor(task, taskListener)(ec))
  }

  case object Start

  case class RunnerResult(item: ServoInitResponseItem, end: DriverCommandEnd)

  def initDrivers(
                   command: DriverCommand,
                   stopNow: AtomicBoolean,
                   logActor: ActorRef,
                   saveDriverLog: Boolean,
                   saveCommandLog: Boolean,
                 )(implicit ec: ExecutionContext): Future[DriverInitResponse] = {
    command.`type` match {
      case Commands.WEB_MONKEY =>
        val params = JsonUtils.mapper.convertValue(command.params, classOf[MonkeyCommandParams])
        val futures = command.servos.map(servo => {
          Future {
            val options = new util.HashMap[String, Object]()
            options.put("start", Boolean.box(false))
            options.put("host", servo.host)
            options.put("port", Int.box(servo.port))
            val meta = command.meta.copy(hostname = servo.hostname)
            val driver = CustomChromeDriver.start(
              options,
              if (saveDriverLog && logActor != null) params => logActor ! DriverDevToolsMessage(meta, params) else null,
              true,
            )
            val item = ServoInitResponseItem(servo, true, null)
            item.runner = WebMonkeyCommandRunner(
              driver, meta, params, stopNow,
              if (saveCommandLog) logActor else null,
              servo.electron
            )
            item
          }.recover {
            case t: Throwable => ServoInitResponseItem(servo, false, t.getMessage)
          }
        })
        Future.sequence(futures).map(items => DriverInitResponse(items))
      case _ => throw new RuntimeException("TBD")
    }
  }

}
