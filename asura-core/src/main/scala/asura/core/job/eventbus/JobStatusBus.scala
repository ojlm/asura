package asura.core.job.eventbus

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import asura.core.job.eventbus.JobStatusBus.JobStatusNotificationMessage

class JobStatusBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

  override type Event = JobStatusNotificationMessage

  override protected def classify(event: JobStatusNotificationMessage): ActorRef = event.ref

  override protected def mapSize: Int = 1
}

object JobStatusBus {

  final case class JobStatusNotificationMessage(ref: ActorRef, operator: String, scheduler: String, jobGroup: String, jobName: String, data: Any = null)

  val EVENT_ADDED = "added"
  val EVENT_SCHEDULED = "scheduled"
  val EVENT_UNSCHEDULED = "unscheduled"
  val EVENT_DELETED = "deleted"
  val EVENT_PAUSED = "paused"
  val EVENT_RESUMED = "resumed"
  val EVENT_RUNNING = "running"
  val EVENT_FINISHED = "finished"
}
