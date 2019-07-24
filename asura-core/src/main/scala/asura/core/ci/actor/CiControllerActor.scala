package asura.core.ci.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, StringUtils}
import asura.core.ci.CiTriggerEventMessage
import asura.core.es.actor.TriggerEventsSaveActor
import asura.core.es.model.TriggerEventLog

class CiControllerActor extends BaseActor {

  private val eventsSave: ActorRef = context.actorOf(TriggerEventsSaveActor.props())

  override def receive: Receive = {
    case msg: CiTriggerEventMessage =>
      if (StringUtils.isNotEmpty(msg.group) && StringUtils.isNotEmpty(msg.project)) {
        // TODO: debounce, trigger
        eventsSave ! TriggerEventLog(
          group = msg.group,
          project = msg.project,
          env = msg.env,
          author = msg.author,
          service = msg.service,
          `type` = msg.`type`,
          timestamp = DateUtils.parse(msg.timestamp)
        )
      }
  }
}

object CiControllerActor {

  def props() = Props(new CiControllerActor())
}
