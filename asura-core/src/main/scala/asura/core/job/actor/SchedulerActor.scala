package asura.core.job.actor

import java.util.Properties

import akka.actor.{ActorRef, Props}
import asura.common.actor.BaseActor
import asura.core.job._

class SchedulerActor(props: Properties*) extends BaseActor {

  override def receive: Receive = {
    case _ => // TODO
  }

  override def preStart(): Unit = {
    SchedulerManager.init(props: _*)
    SchedulerActor.statusMonitor = context.actorOf(JobStatusMonitorActor.props)
  }

  override def postStop(): Unit = SchedulerManager.shutdown()
}

object SchedulerActor {

  def props(props: Properties*) = Props(new SchedulerActor(props: _*))

  var statusMonitor: ActorRef = null
}
