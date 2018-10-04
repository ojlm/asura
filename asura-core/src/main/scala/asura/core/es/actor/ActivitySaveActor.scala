package asura.core.es.actor

import akka.actor.{Props, Status}
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.core.actor.messages.Flush
import asura.core.es.model.Activity
import asura.core.es.service.ActivityService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class ActivitySaveActor extends BaseActor {

  val activities = ArrayBuffer[Activity]()

  override def receive: Receive = {
    case m: Activity =>
      activities += m
      if (activities.length >= 20) {
        insert()
      }
      context.system.scheduler.scheduleOnce(2 seconds) {
        self ! Flush
      }(context.system.dispatcher)
    case Flush =>
      insert()
    case Status.Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
  }

  override def preStart(): Unit = {
  }

  override def postStop(): Unit = {
    insert()
    log.debug(s"${self.path} is stopped")
  }

  private def insert(): Unit = {
    if (activities.length > 0) {
      log.debug(s"${activities.length} activities is saving...")
      ActivityService.index(activities)
      activities.clear()
    }
  }
}

object ActivitySaveActor {

  def props() = Props(new ActivitySaveActor())

}
