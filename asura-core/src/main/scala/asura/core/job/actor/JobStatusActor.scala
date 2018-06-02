package asura.core.job.actor

import akka.actor.Status.Failure
import akka.actor.{ActorRef, Props}
import asura.common.actor._
import asura.common.model.Pagination
import asura.core.actor.messages.SenderMessage
import asura.core.es.model.Job
import asura.core.es.service.JobService
import asura.core.job.actor.JobStatusActor.JobQueryMessage
import asura.core.job.actor.JobStatusMonitorActor.JobStatusOperationMessage
import asura.core.job.eventbus.JobStatusBus.JobStatusNotificationMessage
import asura.core.job.{JobListItem, JobStates}
import asura.core.redis.RedisJobState
import asura.core.util.JacksonSupport
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class JobStatusActor() extends BaseActor {

  var query: JobQueryMessage = null
  val watchIds = mutable.HashSet[String]()

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(query(sender))
  }

  def query(outSender: ActorRef): Receive = {
    case query: JobQueryMessage =>
      this.query = query
      JobService.query(query).map(esResponse => esResponse match {
        case Left(failure) => outSender ! ErrorActorEvent(failure.error.reason)
        case Right(success) =>
          val items = ArrayBuffer[JobListItem]()
          val jobsTable = mutable.HashMap[String, JobListItem]()
          val hits = success.result.hits
          watchIds.clear()
          hits.hits.foreach(hit => {
            val jobId = hit.id
            watchIds.add(jobId)
            jobsTable += (jobId -> {
              val item = JacksonSupport.parse(hit.sourceAsString, classOf[JobListItem])
              item.state = JobStates.UNKNOWN
              items += item
              item._id = jobId
              item
            })
          })
          if (watchIds.nonEmpty) {
            RedisJobState.getJobState(watchIds.toSet).onComplete {
              case util.Success(statesMap) =>
                statesMap.forEach((jobKey, state) => jobsTable(jobKey).state = state)
                outSender ! ListActorEvent(Map("total" -> hits.total, "list" -> items))
              case util.Failure(_) =>
                outSender ! ListActorEvent(Map("total" -> hits.total, "list" -> items))
            }(context.system.dispatcher)
          } else {
            outSender ! ListActorEvent(Map("total" -> 0, "list" -> Nil))
          }
      })(context.system.dispatcher)
    case JobStatusNotificationMessage(_, operator, scheduler, group, name, data) =>
      val jobKey = Job.buildJobKey(scheduler, group, name)
      if (watchIds.contains(jobKey)) {
        outSender ! ItemActorEvent(JobStatusOperationMessage(operator, scheduler, group, name, data))
      }
    case eventMessage: ActorEvent =>
      outSender ! eventMessage
    case Failure(t) =>
      outSender ! ErrorActorEvent(t.getMessage)
  }

  override def postStop(): Unit = {
    import JobStatusActor.logger
    logger.debug(s"JobStatus for ${query} stopped")
  }
}

object JobStatusActor {

  val logger = Logger(classOf[JobStatusActor])

  def props() = Props(new JobStatusActor())

  case class JobQueryMessage(scheduler: String = null, group: String = null, text: String = null) extends Pagination

}
