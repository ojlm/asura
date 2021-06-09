package asura.ui.cli.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.ui.cli.actor.AndroidDeviceActor.{Error, Over, Stdout}
import asura.ui.cli.runner.AndroidRunner.ConfigParams
import asura.ui.cli.utils.{AdbDeviceUtils, AdbUtils}
import asura.ui.cli.window.{DeviceWindow, UiThread}
import se.vidstige.jadb.JadbDevice

class AndroidDeviceActor(
                          device: JadbDevice, params: ConfigParams
                        ) extends BaseActor {

  private var window: DeviceWindow = null

  override def receive: Receive = {
    case Stdout(line) =>
      log.info(s"${device.getSerial}:$line")
    case Error(t) =>
      log.info(s"${device.getSerial} is error: ${LogUtils.stackTraceToString(t)}")
      context stop self
    case Over =>
      log.error(s"connection refused")
      context stop self
    case msg =>
      log.error(s"unknown message type: ${msg.getClass.getSimpleName}")
      context stop self
  }

  private val connection = new Thread(s"connection-${device.getSerial}") {
    override def run(): Unit = {
      AdbDeviceUtils.prepareDevice(device, params.apk)
      AdbUtils.reverse(params.options.socketName, params.serverPort, params.adbPath)
      AdbDeviceUtils.runApp(device, params.options, line => self ! Stdout(line))
      self ! Over
    }
  }

  override def preStart(): Unit = {
    if (params.display) {
      UiThread.run {
        window = DeviceWindow(device.getSerial, params.windowWidth, params.alwaysOnTop)
      }
    }
    connection.start()
  }

  override def postStop(): Unit = {
    if (connection.isAlive) connection.interrupt()
    if (window != null) window.close()
  }

}

object AndroidDeviceActor {

  def props(device: JadbDevice, params: ConfigParams) = {
    Props(new AndroidDeviceActor(device, params))
  }

  case class Stdout(line: String)

  case object Over

  case class Error(t: Throwable)

}


