package asura.ui.cli.actor

import java.util
import java.util.stream.Collectors

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{Props, Terminated}
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.ui.cli.actor.AndroidRunnerActor.ScanDevicesMessage
import asura.ui.cli.runner.AndroidRunner.{ConfigParams, logger}
import se.vidstige.jadb.{JadbConnection, JadbDevice}

class AndroidRunnerActor(
                          params: ConfigParams,
                        )(implicit ec: ExecutionContext) extends BaseActor {

  val connection = new JadbConnection(params.adbHost, params.adbPort)

  override def receive: Receive = {
    case ScanDevicesMessage(devices) =>
      devices.forEach(device => {
        val childOpt = context.child(device.getSerial)
        if (childOpt.isEmpty) {
          val deviceActor = context.actorOf(AndroidDeviceActor.props(device), device.getSerial)
          logger.info(s"watch new device: ${deviceActor.path.name}")
          context.watch(deviceActor)
        }
      })
    case Terminated(actor) =>
      logger.info(s"${actor.path.name} is offline.")
    case msg =>
      log.error(s"Unknown message: ${msg.getClass.getSimpleName}")
  }

  override def preStart(): Unit = {
    if (StringUtils.isNotEmpty(params.serial)) {
      checkDevicesAsync()
    } else {
      context.system.scheduler.scheduleAtFixedRate(0 seconds, params.checkInterval seconds)(() => {
        checkDevicesAsync()
      })
    }
  }

  private def checkDevicesAsync(): Unit = {
    Future {
      try {
        var devices = connection.getDevices
        if (StringUtils.isNotEmpty(params.serial)) {
          devices = devices.stream()
            .filter(device => device.getSerial.equals(params.serial))
            .collect(Collectors.toList[JadbDevice]);
          if (devices.isEmpty) {
            logger.error(s"Can't find device: ${params.serial}")
            sys.exit(0)
          }
        }
        self ! ScanDevicesMessage(devices)
      } catch {
        case t: Throwable =>
          logger.error(s"getDevices error, please confirm the adb server is started: ${t.getMessage}")
      }
    }
  }

}

object AndroidRunnerActor {

  def props(
             params: ConfigParams,
             ec: ExecutionContext,
           ) = Props(new AndroidRunnerActor(params)(ec))

  case class ScanDevicesMessage(devices: util.List[JadbDevice])

}
