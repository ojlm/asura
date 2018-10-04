package asura.core.job.actor

import akka.actor.{Props, Status}
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.core.actor.messages.Flush
import asura.core.es.model.JobReportDataItem
import asura.core.es.service.JobReportDataService
import asura.core.job.actor.JobReportDataItemSaveActor.SaveReportDataItemMessage

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class JobReportDataItemSaveActor(dayIndexSuffix: String) extends BaseActor {

  val messages = ArrayBuffer[SaveReportDataItemMessage]()

  override def receive: Receive = {
    case m: SaveReportDataItemMessage =>
      messages += m
      if (messages.length >= 10) {
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
    if (messages.length > 0) {
      log.debug(s"${messages.length} items is saving...")
      JobReportDataService.index(messages, dayIndexSuffix)
      messages.clear()
    }
  }
}

object JobReportDataItemSaveActor {

  def props(dayIndexSuffix: String) = Props(new JobReportDataItemSaveActor(dayIndexSuffix))

  case class SaveReportDataItemMessage(id: String, dataItem: JobReportDataItem)

}
