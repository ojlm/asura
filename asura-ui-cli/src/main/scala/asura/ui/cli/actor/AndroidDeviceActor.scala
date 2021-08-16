package asura.ui.cli.actor

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.ui.cli.actor.AndroidDeviceActor._
import asura.ui.cli.runner.AndroidRunner.ConfigParams
import asura.ui.cli.utils.{AdbDeviceUtils, AdbUtils}
import asura.ui.cli.window.{DeviceWindow, UiThread}
import asura.ui.hub.Hubs.RenderingFrameHub
import asura.ui.karate.KarateRunner
import com.intuit.karate.Actions
import com.intuit.karate.core.ScenarioEngine
import com.intuit.karate.driver.indigo.IndigoDriver
import se.vidstige.jadb.JadbDevice

class AndroidDeviceActor(
                          device: JadbDevice, params: ConfigParams, ec: ExecutionContext,
                        ) extends BaseActor {

  private implicit val dispatcher = context.dispatcher
  private var window: DeviceWindow = null

  private var engine: ScenarioEngine = null
  private implicit var actions: Actions = null
  private var driver: IndigoDriver = null

  override def receive: Receive = {
    case ExecuteStepMessage(_, step) =>
      runStep(step) pipeTo sender()
    case Stdout(line) =>
      log.info(s"${device.getSerial}:$line")
    case Error(t) =>
      log.info(s"${device.getSerial} is error: ${LogUtils.stackTraceToString(t)}")
      context stop self
    case Over =>
      log.error(s"connection refused")
      if (window != null) {
        RenderingFrameHub.leave(device.getSerial, window)
      }
      context stop self
    case msg =>
      log.error(s"unknown message type: ${msg.getClass.getSimpleName}")
      context stop self
  }

  private def runStep(step: String): Future[ExecuteResult] = {
    Future {
      if (driver == null) {
        engine = ScenarioEngine.forTempUse()
        ScenarioEngine.set(engine)
        actions = KarateRunner.buildScenarioAction(engine)
        driver = IndigoDriver.start(self.path.name, engine.runtime)
        engine.setDriver(driver)
      } else {
        ScenarioEngine.set(engine)
      }
      engine.setFailedReason(null)
      val stepResult = KarateRunner.executeStep(step)
      val result = ExecuteResult(stepResult.passed, if (stepResult.passed) stepResult.result else stepResult.error)
      ScenarioEngine.remove()
      result
    }(ec).recover {
      case t: Throwable => {
        log.error(t, "run step error")
        ExecuteResult(false, s"${t.getClass.getSimpleName}:${t.getMessage}")
      }
    }(ec)
  }

  private val connection = new Thread(s"connection-${device.getSerial}") {
    override def run(): Unit = {
      try {
        AdbDeviceUtils.prepareDevice(device, params.apk)
        AdbUtils.reverse(params.options.socketName, params.serverPort, params.adbPath)
        AdbDeviceUtils.runApp(device, params.options, line => self ! Stdout(line))
      } finally {
        self ! Over
      }
    }
  }

  override def preStart(): Unit = {
    if (params.display) {
      UiThread.run {
        window = DeviceWindow(device.getSerial, params.windowWidth, params.alwaysOnTop)
        connection.start()
      }
    } else {
      connection.start()
    }
  }

  override def postStop(): Unit = {
    if (connection.isAlive) connection.interrupt()
    if (window != null) window.close()
    if (driver != null) driver.close()
  }

}

object AndroidDeviceActor {

  def props(device: JadbDevice, params: ConfigParams, ec: ExecutionContext) = {
    Props(new AndroidDeviceActor(device, params, ec))
  }

  case class Stdout(line: String)

  case object Over

  case class Error(t: Throwable)

  case class ExecuteStepMessage(serial: String, step: String)

  case class ExecuteResult(ok: Boolean, result: Any)

}


