package asura.ui.cli.actor

import java.util
import java.util.stream.Collectors

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{Props, Terminated}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.ui.cli.CliSystem.ACTOR_ASK_TIMEOUT
import asura.ui.cli.actor.AndroidRunnerActor.{GetDevices, ScanDevicesMessage}
import asura.ui.cli.runner.AndroidRunner.ConfigParams
import asura.ui.driver.DriverProvider
import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.driver.indigo.IndigoDriver
import com.intuit.karate.driver.{Driver, DriverOptions}
import se.vidstige.jadb.{JadbConnection, JadbDevice}

class AndroidRunnerActor(
                          params: ConfigParams,
                        )(implicit ec: ExecutionContext) extends BaseActor with DriverProvider {

  val connection = new JadbConnection(params.adbHost, params.adbPort)

  override def receive: Receive = {
    case ScanDevicesMessage(devices) =>
      devices.forEach(device => {
        val childOpt = context.child(device.getSerial)
        if (childOpt.isEmpty) {
          val deviceActor = context.actorOf(AndroidDeviceActor.props(device, params, ec), device.getSerial)
          log.info(s"watch new device: ${deviceActor.path.name}")
          context.watch(deviceActor)
        }
      })
    case GetDevices =>
      sender() ! context.children.map(_.path.name).toSeq
    case msg: AndroidDeviceActor.ExecuteStepMessage =>
      val childOpt = context.child(msg.serial)
      if (childOpt.nonEmpty) {
        (childOpt.get ? msg) pipeTo sender()
      } else {
        sender() ! AndroidDeviceActor.ExecuteResult(false, s"${msg.serial} is not available")
      }
    case Terminated(actor) =>
      log.info(s"${actor.path.name} is offline.")
    case msg =>
      log.error(s"Unknown message: ${msg.getClass.getSimpleName}")
  }

  override def preStart(): Unit = {
    DriverOptions.setDriverProvider(this)
    if (StringUtils.isNotEmpty(params.serial)) {
      checkDevicesAsync()
    } else {
      context.system.scheduler.scheduleAtFixedRate(0 seconds, params.checkInterval seconds)(() => {
        checkDevicesAsync()
      })
    }
  }

  override def get(options: util.Map[String, AnyRef], sr: ScenarioRuntime): Driver = {
    if (!options.containsKey("serial")) {
      val devices = context.children.iterator
      if (devices.hasNext) {
        val serial = devices.next().path.name
        log.info(s"Will use device $serial")
        options.put("serial", serial)
      } else {
        throw new RuntimeException("There is no device available")
      }
    }
    IndigoDriver.start(options, sr)
  }

  override def release(driver: Driver): Unit = {
  }

  private def checkDevicesAsync(): Unit = {
    Future {
      try {
        var devices = connection.getDevices
        if (StringUtils.isNotEmpty(params.serial)) {
          devices = devices.stream()
            .filter(device => device.getSerial.equals(params.serial))
            .collect(Collectors.toList[JadbDevice])
          if (devices.isEmpty) {
            log.error(s"Can't find device: ${params.serial}")
            sys.exit(0)
          }
        }
        self ! ScanDevicesMessage(devices)
      } catch {
        case t: Throwable =>
          log.error(s"getDevices error, please confirm the adb server is started: ${t.getMessage}")
      }
    }
  }

}

object AndroidRunnerActor {

  def props(
             params: ConfigParams,
             ec: ExecutionContext,
           ) = Props(new AndroidRunnerActor(params)(ec))

  case object GetDevices

  case class ScanDevicesMessage(devices: util.List[JadbDevice])

}
