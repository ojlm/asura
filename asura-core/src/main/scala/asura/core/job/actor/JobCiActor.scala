package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{LogUtils, StringUtils}
import asura.core.CoreConfig
import asura.core.actor.messages.SenderMessage
import asura.core.cs.ContextOptions
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

  def handleRequest(webActor: ActorRef): Receive = {
    case job: Job =>
      val jobImplOpt = JobCenter.classAliasJobMap.get(job.classAlias)
      if (jobImplOpt.isEmpty) {
        webActor ! s"Can't find job implementation of ${job.classAlias}"
        webActor ! JobExecDesc.STATUS_FAIL
        webActor ! PoisonPill
      } else {
        val jobImpl = jobImplOpt.get
        val (isOk, errMsg) = jobImpl.checkJobData(job.jobData)
        if (isOk) {
          jobImpl.doTestAsync(JobExecDesc.from(id, job, JobReport.TYPE_CI, options), logMsg => {
            webActor ! logMsg
          }).pipeTo(self)
        } else {
          webActor ! errMsg
          webActor ! PoisonPill
        }
      }
    case jobId: String =>
      if (StringUtils.isNotEmpty(jobId)) {
        JobService.geJobById(jobId).pipeTo(self)
      } else {
        webActor ! s"jobId is empty."
        webActor ! PoisonPill
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      report.fillCommonFields(BaseIndex.CREATOR_CI)
      JobReportService.index(report).map { res =>
        val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${res.id}"
        webActor ! reportUrl
        webActor ! execDesc.report.result
        webActor ! PoisonPill
      }
    case Status.Failure(t) =>
      val stackTrace = LogUtils.stackTraceToString(t)
      log.warning(stackTrace)
      webActor ! stackTrace
      webActor ! JobExecDesc.STATUS_FAIL
      webActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobCiActor {
  def props(id: String, out: ActorRef = null, options: ContextOptions = null) = Props(new JobCiActor(id, out, options))
}
