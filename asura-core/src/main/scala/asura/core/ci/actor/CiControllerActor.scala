package asura.core.ci.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.core.ci.CiTriggerEventMessage

class CiControllerActor extends BaseActor {

  override def receive: Receive = {
    case msg: CiTriggerEventMessage =>
      log.debug(msg.toString)
      if (StringUtils.isNotEmpty(msg.group) && StringUtils.isNotEmpty(msg.project)) {

      }
  }
}

object CiControllerActor {

  def props() = Props(new CiControllerActor())
}
