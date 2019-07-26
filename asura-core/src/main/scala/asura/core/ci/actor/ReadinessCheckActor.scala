package asura.core.ci.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.core.ci.actor.ReadinessCheckActor.DoCheck
import asura.core.es.model.CiTrigger.ReadinessCheck

class ReadinessCheckActor(readiness: ReadinessCheck) extends BaseActor {

  override def receive: Receive = {
    case DoCheck =>
      // TODO
      sender() ! (true, "ok")
  }
}

object ReadinessCheckActor {

  def props(readiness: ReadinessCheck) = Props(new ReadinessCheckActor(readiness))

  final object DoCheck

}
