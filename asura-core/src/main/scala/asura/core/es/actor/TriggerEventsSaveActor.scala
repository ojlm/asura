package asura.core.es.actor

import akka.actor.{Props, Status}
import asura.common.actor.BaseActor
import asura.common.util.LogUtils
import asura.core.actor.messages.Flush
import asura.core.es.model.TriggerEventLog
import asura.core.es.service.TriggerEventLogService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class TriggerEventsSaveActor extends BaseActor {

  val logs = ArrayBuffer[TriggerEventLog]()

  override def receive: Receive = {
    case m: TriggerEventLog =>
      logs += m
      if (logs.length >= 20) {
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
    if (logs.length > 0) {
      log.debug(s"${logs.length} trigger events is saving...")
      TriggerEventLogService.index(logs)
      logs.clear()
    }
  }
}

object TriggerEventsSaveActor {
  def props() = Props(new TriggerEventsSaveActor())
}
