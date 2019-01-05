package asura.dubbo.actor

import asura.common.actor.BaseActor

class GenericServiceInvokerActor extends BaseActor {

  override def receive: Receive = {
    case _ =>
  }

  override def postStop(): Unit = super.postStop()
}
