package asura.core.ci.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.core.ci.CiTriggerEventMessage

class CiControllerActor extends BaseActor {

  override def receive: Receive = {
    case CiTriggerEventMessage =>
  }
}

object CiControllerActor {

  def props() = Props(new CiControllerActor())
}
