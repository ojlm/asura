package asura.core.ci.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.core.ci.CiTriggerEventMessage
import asura.core.es.actor.TriggerEventsSaveActor

// singleton
class CiControllerActor extends BaseActor {

  private val eventsSave: ActorRef = context.actorOf(TriggerEventsSaveActor.props())

  override def receive: Receive = {
    case msg: CiTriggerEventMessage =>
      if (StringUtils.isNotEmpty(msg.group) && StringUtils.isNotEmpty(msg.project)) {
        val key = msg.eventKey
        val worker = context
          .child(key)
          .getOrElse(context.actorOf(CiEventHandlerActor.props(eventsSave, msg.service), key))
        worker ! msg
      }
  }
}

object CiControllerActor {

  def props() = Props(new CiControllerActor())
}
