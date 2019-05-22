package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{LogUtils, XtermUtils}
import asura.core.CoreConfig
import asura.core.es.model.{JobData, JobReport, VariablesImportItem}
import asura.core.es.service.JobReportService
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.{JobCenter, JobExecDesc, JobMeta}
import asura.core.runtime.ContextOptions

class JobTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_JOB_TIMEOUT
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case JobTestMessage(jobId, jobMeta, jobData, imports) =>
      val jobOpt = JobCenter.classAliasJobMap.get(jobMeta.getJobAlias())
      if (jobOpt.isEmpty) {
        wsActor ! ErrorActorEvent(s"Can't find job implementation of ${jobMeta.getJobAlias()}")
        wsActor ! Status.Success
      } else {
        val job = jobOpt.get
        val (isOk, errMsg) = job.checkJobData(jobData)
        if (isOk) {
          JobExecDesc.from(
            jobId,
            jobMeta,
            jobData,
            JobReport.TYPE_TEST,
            ContextOptions(jobEnv = jobMeta.env),
            user,
            imports
          ).map(jobExecDesc => {
            val runner = context.actorOf(JobRunnerActor.props(wsActor))
            (runner.ask(jobExecDesc)(timeout, self)) pipeTo self
          }).recover {
            case t: Throwable =>
              self ! Status.Failure(t)
          }
        } else {
          wsActor ! ErrorActorEvent(errMsg)
          wsActor ! Status.Success
        }
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      JobReportService.indexReport(execDesc.reportId, report).map { res =>
        if (report.isSuccessful()) {
          wsActor ! NotifyActorEvent(s"\n[JOB][${report.jobName}]: ${XtermUtils.greenWrap(report.result)}")
        } else {
          wsActor ! NotifyActorEvent(s"\n[JOB][${report.jobName}]: ${XtermUtils.redWrap(report.result)}")
        }
        val reportUrl = s"[REPORT]: ${CoreConfig.reportBaseUrl}/${report.group}/${report.project}/${res.id}"
        wsActor ! NotifyActorEvent(reportUrl)
        wsActor ! OverActorEvent(report)
        wsActor ! Status.Success
      }.recover {
        case t: Throwable =>
          self ! Status.Failure(t)
      }
    case eventMessage: ActorEvent =>
      wsActor ! eventMessage
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      log.warning(errLog)
      wsActor ! ErrorActorEvent(t.getMessage)
      wsActor ! Status.Success
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobTestActor {

  def props(user: String, out: ActorRef = null) = Props(new JobTestActor(user, out))

  case class JobTestMessage(jobId: String, jobMeta: JobMeta, jobData: JobData, imports: Seq[VariablesImportItem])

}
