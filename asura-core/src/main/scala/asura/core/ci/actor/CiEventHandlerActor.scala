package asura.core.ci.actor

import akka.actor.Status.Failure
import akka.actor.{ActorRef, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, LogUtils, StringUtils}
import asura.core.ci.CiTriggerEventMessage
import asura.core.es.model.TriggerEventLog.ExtData
import asura.core.es.model.{CiTrigger, TriggerEventLog}
import asura.core.es.service.CiTriggerService
import asura.core.model.QueryTrigger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

// one key, one actor
class CiEventHandlerActor(eventsSave: ActorRef) extends BaseActor {

  implicit val ec = context.dispatcher

  var stage = CiEventHandlerActor.STAGE_INIT
  var initialMessages = ArrayBuffer[CiTriggerEventMessage]() // buffers when triggers are not ready
  var triggers: Map[String, CiTrigger] = null
  val triggerLastTime = mutable.Map[String, Long]()

  override def receive: Receive = {
    case msg: CiTriggerEventMessage =>
      this.stage match {
        case CiEventHandlerActor.STAGE_INIT =>
          this.initialMessages += msg
          val q = QueryTrigger(msg.group, msg.project, msg.env, msg.service, true.toString, null)
          q.size = CiEventHandlerActor.MAX_COUNT
          this.stage = CiEventHandlerActor.STAGE_FETCHING
          CiTriggerService.getTriggersAsMap(q) pipeTo self
        case CiEventHandlerActor.STAGE_FETCHING =>
          this.initialMessages += msg
        case CiEventHandlerActor.STAGE_READY =>
          consume(msg)
      }
    case triggers: Map[String, CiTrigger] =>
      this.triggers = triggers
      this.stage = CiEventHandlerActor.STAGE_READY
      this.initialMessages.foreach(consume)
      this.initialMessages = null // clear
    case Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
      context.stop(self)
  }

  private def consume(msg: CiTriggerEventMessage): Unit = {
    if (null != this.triggers && this.triggers.nonEmpty) {
      this.triggers.foreach(tuple => {
        val (docId, trigger) = tuple
        if (!debounce(docId, trigger)) {
          val readiness = trigger.readiness
          val serviceReady = if (null != readiness && readiness.enabled) {
            context.actorOf(ReadinessCheckActor.props(readiness))
              .ask(ReadinessCheckActor.DoCheck)(readiness.totalTimeout)
              .asInstanceOf[Future[(Boolean, String)]]
          } else {
            Future.successful((true, StringUtils.EMPTY))
          }
          serviceReady.recover {
            case t: Throwable =>
              val errMsg = LogUtils.stackTraceToString(t)
              log.warning(errMsg)
              (false, errMsg)
          }.map(serviceStatus => {
            serviceStatus match {
              case (true, _) => // service is running
              // TODO: do the job
              case (false, errMsg) => // something wrong
                sendLog(msg, docId, trigger, TriggerEventLog.RESULT_ILL, ExtData(errMsg))
            }
          })
        } else {
          // debounce
          sendLog(msg, docId, trigger, TriggerEventLog.RESULT_DEBOUNCE)
        }
      })
    } else {
      // missing trigger
      eventsSave ! TriggerEventLog(
        group = msg.group,
        project = msg.project,
        env = msg.env,
        author = msg.author,
        service = msg.service,
        `type` = msg.`type`,
        timestamp = DateUtils.parse(msg.timestamp),
        result = TriggerEventLog.RESULT_MISS,
      )
      // stop self immediately, stay for more executions ？
      context.stop(self)
    }
  }

  private def sendLog(
                       msg: CiTriggerEventMessage,
                       docId: String,
                       trigger: CiTrigger,
                       result: String,
                       ext: ExtData = null,
                     ): Unit = {
    eventsSave ! TriggerEventLog(
      group = msg.group,
      project = msg.project,
      env = msg.env,
      author = msg.author,
      service = msg.service,
      `type` = msg.`type`,
      timestamp = DateUtils.parse(msg.timestamp),
      result = result,
      triggerId = docId,
      targetType = trigger.targetType,
      targetId = trigger.targetId,
      ext = ext,
    )
  }

  private def debounce(docId: String, trigger: CiTrigger): Boolean = {
    val lastOpt = this.triggerLastTime.get(docId)
    if (lastOpt.nonEmpty) {
      if ((System.nanoTime() / 1000000 - lastOpt.get) > trigger.debounce) {
        false
      } else {
        true
      }
    } else {
      this.triggerLastTime.put(docId, System.nanoTime() / 1000000)
      false
    }
  }
}

object CiEventHandlerActor {

  def props(eventsSave: ActorRef) = Props(new CiEventHandlerActor(eventsSave))

  val MAX_COUNT = 100
  val STAGE_INIT = 0 // initial state
  val STAGE_FETCHING = 1 // get trigger data from es
  val STAGE_READY = 2 // trigger data
}
