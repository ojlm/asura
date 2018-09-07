package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{LogUtils, StringUtils}
import asura.core.CoreConfig
import asura.core.actor.messages.SenderMessage
import asura.core.es.model.{Job, JobReport}
import asura.core.es.service.{JobReportService, JobService}
import asura.core.job.{JobCenter, JobExecDesc}

class JobManualActor(jobId: String, user: String, out: ActorRef) extends BaseActor {

  implicit val executionContext = context.dispatcher
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
      self ! jobId
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
          jobImpl.doTestAsync(JobExecDesc.from(job, JobReport.TYPE_MANUAL, null), logMsg => {
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
      report.fillCommonFields(user)
      JobReportService.index(report).map { res =>
        res match {
          case Left(failure) =>
            val errMsg = s"save job report fail: ${failure.error.reason}"
            webActor ! errMsg
          case Right(success) =>
            val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${success.result.id}"
            webActor ! reportUrl
        }
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

object JobManualActor {
  def props(jobId: String, user: String, out: ActorRef = null) = Props(new JobManualActor(jobId, user, out))
}
