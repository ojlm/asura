package asura.core.job.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.BaseActor
import asura.core.job.eventbus.JobStatusBus

class JobStatusMonitorActor extends BaseActor {

  val statusChangeBus = new JobStatusBus(context.system)

  override def receive: Receive = {
    //    case JobStatusSubscribeMessage(ref: ActorRef) =>
    //      statusChangeBus.subscribe(ref, self)
    //    case JobAdded(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.NORMAL) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_ADDED, scheduler, group, name))
    //      }
    //    case JobDeleted(scheduler, group, name) =>
    //      RedisJobState.deleteJobState(scheduler, group, name) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_DELETED, scheduler, group, name))
    //      }
    //    case JobPaused(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.PAUSED) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_PAUSED, scheduler, group, name))
    //      }
    //    case JobResumed(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.NORMAL) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_RESUMED, scheduler, group, name))
    //      }
    //    case JobRunning(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.RUNNING) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_RUNNING, scheduler, group, name))
    //      }
    //    case JobScheduled(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.NORMAL) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_SCHEDULED, scheduler, group, name))
    //      }
    //    case JobUnscheduled(scheduler, group, name) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.NORMAL) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_UNSCHEDULED, scheduler, group, name))
    //      }
    //    case JobFinished(scheduler, group, name, report) =>
    //      RedisJobState.updateJobState(scheduler, group, name, JobStates.NORMAL) {
    //        statusChangeBus.publish(JobStatusNotificationMessage(self, JobStatusBus.EVENT_FINISHED, scheduler, group, name, report))
    //      }
    case _ =>
  }
}


object JobStatusMonitorActor {

  def props = Props(new JobStatusMonitorActor())

  case class JobStatusSubscribeMessage(ref: ActorRef)

  case class JobStatusOperationMessage(operator: String, scheduler: String, jobGroup: String, jobName: String, data: Any)

}
