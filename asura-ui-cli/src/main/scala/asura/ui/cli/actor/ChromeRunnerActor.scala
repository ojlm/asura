package asura.ui.cli.actor

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, JsonUtils, LogUtils, StringUtils}
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.cli.CliSystem
import asura.ui.cli.runner.MonkeyRunner.ConfigParams
import asura.ui.command.{Commands, WebMonkeyCommandRunner}
import asura.ui.driver._
import asura.ui.model.BytesObject

import scala.concurrent.{ExecutionContext, Future}

class ChromeRunnerActor(
                         params: ConfigParams,
                         options: util.HashMap[String, Object],
                         electron: Boolean,
                       )(implicit ec: ExecutionContext) extends BaseActor {

  val stopNow = new AtomicBoolean(false)
  var commandLogFile: BufferedWriter = null
  var driverLogFile: BufferedWriter = null
  var driver: CustomChromeDriver = null

  override def receive: Receive = {
    case NewDriver(driver, _) =>
      if (null != driver) {
        this.driver = driver
        run()
      } else {
        quit()
      }
    case DriverDevToolsMessage(_, params) =>
      val paramsStr = JsonUtils.stringify(params)
      log.info(s"driver: $paramsStr")
      if (driverLogFile != null) {
        driverLogFile.write(DateUtils.parse(System.currentTimeMillis(), CliSystem.LOG_DATE_PATTERN))
        driverLogFile.write(" ")
        driverLogFile.write(paramsStr)
        driverLogFile.newLine()
      }
    case DriverCommandLog(_, ty, params, _, timestamp) =>
      val paramsStr = ty match {
        case DriverCommandLog.TYPE_SCREEN => s"size: ${params.asInstanceOf[BytesObject].bytes.length}"
        case DriverCommandLog.TYPE_MOUSE => JsonUtils.stringify(params)
        case DriverCommandLog.TYPE_KEYBOARD => params.toString
        case DriverCommandLog.TYPE_LOG => params.toString
      }
      log.info(s"command: $ty $paramsStr")
      if (commandLogFile != null) {
        commandLogFile.write(DateUtils.parse(timestamp, CliSystem.LOG_DATE_PATTERN))
        commandLogFile.write(" ")
        commandLogFile.write(ty)
        commandLogFile.write(" ")
        commandLogFile.write(paramsStr)
        commandLogFile.newLine()
      }
    case _: DriverCommandEnd =>
      quit()
    case msg =>
      log.error(s"Unknown message: ${msg.getClass.getSimpleName}")
      quit()
  }

  def run(): Unit = {
    Future {
      val runner = WebMonkeyCommandRunner(driver, null, params.command.params, stopNow, self, electron)
      runner.run()
    }.recover {
      case t: Throwable =>
        log.warning(LogUtils.stackTraceToString(t))
        DriverCommandEnd(Commands.WEB_MONKEY, false, t.getMessage)
    } pipeTo self
  }

  override def preStart(): Unit = {
    Future {
      if (StringUtils.isNotEmpty(params.command.logFile)) {
        commandLogFile = new BufferedWriter(new FileWriter(
          new File(params.command.logFile).getAbsoluteFile, StandardCharsets.UTF_8, true
        ))
      }
      if (StringUtils.isNotEmpty(params.driver.logFile)) {
        driverLogFile = new BufferedWriter(new FileWriter(
          new File(params.driver.logFile).getAbsoluteFile, StandardCharsets.UTF_8, true
        ))
      }
      val sendFunc = (params: util.Map[String, AnyRef]) => {
        self ! DriverDevToolsMessage(null, params)
      }
      NewDriver(CustomChromeDriver.start(options, sendFunc, true), null)
    }.recover({
      case t: Throwable =>
        log.error(LogUtils.stackTraceToString(t))
        NewDriver(null, null)
    }).pipeTo(self)
  }

  def quit(): Unit = {
    if (commandLogFile != null) commandLogFile.close()
    if (driverLogFile != null) driverLogFile.close()
    if (driver != null) driver.realQuit()
    CliSystem.system.terminate()
  }

}

object ChromeRunnerActor {

  def props(
             params: ConfigParams,
             options: util.HashMap[String, Object],
             electron: Boolean,
             ec: ExecutionContext,
           ) = Props(new ChromeRunnerActor(params, options, electron)(ec))

}

