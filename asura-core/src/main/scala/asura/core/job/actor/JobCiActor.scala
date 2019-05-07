package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor.{BaseActor, SenderMessage}
import asura.common.util.{LogUtils, StringUtils}
import asura.core.CoreConfig
import asura.core.runtime.ContextOptions
import asura.core.es.model.{BaseIndex, Job, JobReport}
import asura.core.es.service.{JobReportService, JobService}
import asura.core.job.{JobCenter, JobExecDesc}

class JobCiActor(id: String, out: ActorRef, options: ContextOptions) extends BaseActor {

  implicit val executionContext = context.dispatcher
  if (null != out) {
    self ! SenderMessage(out)
  }

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
      self ! id
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case job: Job =>
      val jobImplOpt = JobCenter.classAliasJobMap.get(job.classAlias)
      if (jobImplOpt.isEmpty) {
        wsActor ! s"Can't find job implementation of ${job.classAlias}"
        wsActor ! JobExecDesc.STATUS_FAIL
        wsActor ! PoisonPill
      } else {
        val jobImpl = jobImplOpt.get
        val (isOk, errMsg) = jobImpl.checkJobData(job.jobData)
        if (isOk) {
          JobExecDesc.from(id, job, JobReport.TYPE_CI, options, BaseIndex.CREATOR_CI).map(jobExecDesc => {
            jobImpl.doTestAsync(jobExecDesc, logMsg => {
              wsActor ! logMsg
            }).pipeTo(self)
          }).recover {
            case t: Throwable =>
              self ! Status.Failure(t)
          }
        } else {
          wsActor ! errMsg
          wsActor ! PoisonPill
        }
      }
    case jobId: String =>
      if (StringUtils.isNotEmpty(jobId)) {
        JobService.geJobById(jobId).pipeTo(self)
      } else {
        wsActor ! s"jobId is empty."
        wsActor ! PoisonPill
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      JobReportService.indexReport(execDesc.reportId, report).map { _ =>
        val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${execDesc.reportId}"
        wsActor ! reportUrl
        wsActor ! execDesc.report.result
        wsActor ! PoisonPill
      }.recover {
        case t: Throwable =>
          self ! Status.Failure(t)
      }
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      wsActor ! t.getMessage
      wsActor ! JobExecDesc.STATUS_FAIL
      wsActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobCiActor {
  def props(id: String, out: ActorRef = null, options: ContextOptions = null) = Props(new JobCiActor(id, out, options))
}
