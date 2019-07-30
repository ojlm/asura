package asura.core.job.actor

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{LogUtils, XtermUtils}
import asura.core.CoreConfig
import asura.core.es.model.{BaseIndex, Job, JobReport}
import asura.core.es.service.{JobNotifyService, JobReportService, JobService}
import asura.core.job.{JobCenter, JobExecDesc}
import asura.core.runtime.ContextOptions

class JobCiActor(id: String, wsActor: ActorRef, options: ContextOptions) extends BaseActor {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_JOB_TIMEOUT

  var sseActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(sender) =>
      sseActor = sender
      self ! id
    case jobId: String =>
      JobService.getJobById(jobId).pipeTo(self)
    case wsEvent: ActorEvent => // replay to sse actor, from JobRunnerActor
      if (null != sseActor && ActorEvent.TYPE_NOTIFY.equals(wsEvent.`type`)) {
        sseActor ! wsEvent.msg
      }
    case job: Job =>
      val jobImpl = JobCenter.classAliasJobMap.get(job.classAlias).get
      val (isOk, errMsg) = jobImpl.checkJobData(job.jobData)
      if (isOk) {
        JobExecDesc.from(id, job, JobReport.TYPE_CI, options, BaseIndex.CREATOR_CI).map(jobExecDesc => {
          val eventLogRef = if (null != wsActor) wsActor else self
          val jobRunnerActor = context.actorOf(JobRunnerActor.props(eventLogRef, null))
          (jobRunnerActor ? jobExecDesc) pipeTo self
        }).recover {
          case t: Throwable =>
            self ! Status.Failure(t)
        }
      } else {
        if (null != sseActor) {
          sseActor ! errMsg
          sseActor ! Status.Success
        }
        if (null != wsActor) {
          wsActor ! ErrorActorEvent(errMsg)
          wsActor ! Status.Success
        }
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      JobReportService.indexReport(execDesc.reportId, report).map { res =>
        JobNotifyService.notifySubscribers(execDesc)
        val reportStatus = if (report.isSuccessful()) {
          s"[JOB][${report.jobName}]: ${XtermUtils.greenWrap(report.result)}"
        } else {
          s"[JOB][${report.jobName}]: ${XtermUtils.redWrap(report.result)}"
        }
        val reportReport = s"[REPORT]: ${CoreConfig.reportBaseUrl}/${report.group}/${report.project}/${res.id}"
        if (null != sseActor) {
          sseActor ! reportStatus
          sseActor ! reportReport
          sseActor ! execDesc.report.result
          sseActor ! Status.Success
        }
        if (null != wsActor) {
          wsActor ! NotifyActorEvent(reportStatus)
          wsActor ! NotifyActorEvent(reportReport)
          wsActor ! NotifyActorEvent(execDesc.report.result)
          wsActor ! Status.Success
        }
        context.stop(self)
      }.recover {
        case t: Throwable => self ! Status.Failure(t)
      }
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      if (null != sseActor) {
        sseActor ! t.getMessage
        sseActor ! JobExecDesc.STATUS_FAIL
        sseActor ! Status.Success
      }
      if (null != wsActor) {
        wsActor ! ErrorActorEvent(t.getMessage)
        wsActor ! NotifyActorEvent(JobExecDesc.STATUS_FAIL)
        wsActor ! Status.Success
      }
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobCiActor {

  def props(id: String, out: ActorRef = null, options: ContextOptions = null) = Props(new JobCiActor(id, out, options))
}
