package asura.core.ci.actor

import akka.actor.Status.Failure
import akka.actor.{ActorRef, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, LogUtils, StringUtils}
import asura.core.CoreConfig
import asura.core.ci.CiTriggerEventMessage
import asura.core.ci.actor.CiEventHandlerActor.DecreaseTrigger
import asura.core.es.model.TriggerEventLog.ExtData
import asura.core.es.model._
import asura.core.es.service.{CiTriggerService, JobNotifyService, JobReportService, JobService}
import asura.core.job.actor.JobRunnerActor
import asura.core.job.{JobCenter, JobExecDesc}
import asura.core.model.QueryTrigger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

// one key, one actor
class CiEventHandlerActor(eventsSave: ActorRef, service: String) extends BaseActor {

  implicit val ec = context.dispatcher

  var stage = CiEventHandlerActor.STAGE_INIT
  var initialMessages = ArrayBuffer[CiTriggerEventMessage]() // buffers when triggers are not ready
  var triggers: Map[String, CiTrigger] = Map.empty
  val triggerLastTime = mutable.Map[String, Long]()
  var leftTriggerCount = 0 // control when to stop

  override def receive: Receive = {
    case msg: CiTriggerEventMessage =>
      this.stage match {
        case CiEventHandlerActor.STAGE_INIT =>
          this.initialMessages += msg
          val q = QueryTrigger(msg.group, msg.project, msg.env, null, true.toString, null)
          q.size = CiEventHandlerActor.MAX_COUNT
          this.stage = CiEventHandlerActor.STAGE_FETCHING
          CiTriggerService.getTriggersAsMap(q) pipeTo self
        case CiEventHandlerActor.STAGE_FETCHING =>
          this.initialMessages += msg
        case CiEventHandlerActor.STAGE_READY =>
          this.leftTriggerCount = this.leftTriggerCount + this.triggers.size
          consume(msg)
      }
    case triggers: Map[_, _] =>
      this.stage = CiEventHandlerActor.STAGE_READY
      this.triggers = triggers.asInstanceOf[Map[String, CiTrigger]].filter(tuple => filterTrigger(tuple._2))
      this.leftTriggerCount = this.triggers.size * this.initialMessages.size
      this.initialMessages.foreach(consume)
      this.initialMessages = null // clear
    case DecreaseTrigger =>
      this.leftTriggerCount = this.leftTriggerCount - 1
      this.tryStopSelf()
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
              .ask(ReadinessCheckActor.DoCheck)(CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT)
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
                doJobSendLogAndDecrease(msg, docId, trigger)
              case (false, errMsg) => // something wrong
                sendLogAndDecrease(msg, docId, trigger, TriggerEventLog.RESULT_ILL, ext = ExtData(errMsg))
            }
          })
        } else {
          // debounce
          sendLogAndDecrease(msg, docId, trigger, TriggerEventLog.RESULT_DEBOUNCE)
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
      decreaseAndTryStop()
    }
  }

  private def doJobSendLogAndDecrease(
                                       msg: CiTriggerEventMessage,
                                       docId: String,
                                       trigger: CiTrigger
                                     ): Unit = {
    trigger.targetType match {
      case ScenarioStep.TYPE_JOB =>
        JobService.getJobById(trigger.targetId).onComplete {
          case scala.util.Success(job) =>
            try {
              val jobImpl = JobCenter.classAliasJobMap.get(job.classAlias).get
              val (isOk, errMsg) = jobImpl.checkJobData(job.jobData)
              if (isOk) {
                JobExecDesc.from(trigger.targetId, job, JobReport.TYPE_CI, null, BaseIndex.CREATOR_CI)
                  .flatMap(jobExecDesc => {
                    context.actorOf(JobRunnerActor.props(null, null))
                      .ask(jobExecDesc)(CoreConfig.DEFAULT_JOB_TIMEOUT)
                  })
                  .flatMap(answer => {
                    val execDesc = answer.asInstanceOf[JobExecDesc]
                    execDesc.prepareEnd()
                    val report = execDesc.report
                    JobReportService.indexReport(execDesc.reportId, report).map(_ => {
                      JobNotifyService.notifySubscribers(execDesc)
                      execDesc
                    })
                  })
                  .onComplete {
                    case scala.util.Success(execDesc) =>
                      sendLogAndDecrease(msg, docId, trigger, execDesc.report.result, job.group, job.project, execDesc.reportId)
                    case util.Failure(t) =>
                      sendLogAndDecreaseWithError(msg, docId, trigger, t)
                  }
              } else {
                sendLogAndDecrease(
                  msg,
                  docId,
                  trigger,
                  TriggerEventLog.RESULT_ERROR,
                  ext = ExtData(errMsg)
                )
              }
            } catch {
              case t: Throwable => sendLogAndDecreaseWithError(msg, docId, trigger, t)
            }
          case util.Failure(t) => sendLogAndDecreaseWithError(msg, docId, trigger, t)
        }
      case _ =>
        sendLogAndDecrease(
          msg,
          docId,
          trigger,
          TriggerEventLog.RESULT_UNKNOWN,
          ext = ExtData(trigger.targetType)
        )
    }
  }

  @inline
  private def sendLogAndDecreaseWithError(
                                           msg: CiTriggerEventMessage,
                                           docId: String,
                                           trigger: CiTrigger,
                                           t: Throwable
                                         ): Unit = {
    sendLogAndDecrease(
      msg,
      docId,
      trigger,
      TriggerEventLog.RESULT_ERROR,
      ext = ExtData(LogUtils.stackTraceToString(t))
    )
  }

  @inline
  private def decreaseAndTryStop(): Unit = {
    self ! DecreaseTrigger
  }

  private def tryStopSelf(): Unit = {
    // stop self immediately, stay for more executions ?
    if (this.leftTriggerCount < 1) {
      context.stop(self)
    }
  }

  private def sendLogAndDecrease(
                                  msg: CiTriggerEventMessage,
                                  docId: String,
                                  trigger: CiTrigger,
                                  result: String,
                                  targetGroup: String = StringUtils.EMPTY,
                                  targetProject: String = StringUtils.EMPTY,
                                  reportId: String = StringUtils.EMPTY,
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
      targetGroup = targetGroup,
      targetProject = targetProject,
      targetId = trigger.targetId,
      reportId = reportId,
      ext = ext,
    )
    decreaseAndTryStop()
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

  // If the trigger set a service value, then it must be the prefix of the same field in event
  private def filterTrigger(trigger: CiTrigger): Boolean = {
    if (StringUtils.isNotEmpty(trigger.service) && StringUtils.isNotEmpty(service)) {
      service.startsWith(trigger.service)
    } else {
      true
    }
  }
}

object CiEventHandlerActor {

  def props(eventsSave: ActorRef, service: String) = Props(new CiEventHandlerActor(eventsSave, service))

  val MAX_COUNT = 100
  val STAGE_INIT = 0 // initial state
  val STAGE_FETCHING = 1 // get trigger data from es
  val STAGE_READY = 2 // trigger data

  final object DecreaseTrigger

}
