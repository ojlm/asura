package asura.core.ci

import akka.actor.{ActorRef, ActorSystem}
import asura.core.ci.actor.CiControllerActor

object CiManager {

  private var ciActor: ActorRef = null

  def init(system: ActorSystem): Unit = {
    ciActor = system.actorOf(CiControllerActor.props())
  }

  def eventSource(msg: CiTriggerEventMessage): Unit = {
    if (null != ciActor) {
      ciActor ! msg
    }
  }
}
