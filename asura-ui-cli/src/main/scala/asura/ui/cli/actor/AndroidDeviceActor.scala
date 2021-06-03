package asura.ui.cli.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import se.vidstige.jadb.JadbDevice

class AndroidDeviceActor(
                          device: JadbDevice
                        ) extends BaseActor {

  override def receive: Receive = {
    case msg =>
      log.error(s"Unknown message: ${msg.getClass.getSimpleName}")
  }

  override def preStart(): Unit = {
    // TODO
  }

  override def postStop(): Unit = {
    // TODO
  }
}

object AndroidDeviceActor {

  def props(device: JadbDevice) = Props(new AndroidDeviceActor(device))

}


