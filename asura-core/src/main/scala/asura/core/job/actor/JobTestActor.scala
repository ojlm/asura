package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.common.util.LogUtils
import asura.core.CoreConfig
import asura.core.actor.messages.SenderMessage
import asura.core.es.model.{JobData, JobReport}
import asura.core.es.service.JobReportService
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.{JobCenter, JobExecDesc, JobMeta}

class JobTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val executionContext = context.dispatcher
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case JobTestMessage(jobMeta, jobData) =>
      val jobOpt = JobCenter.classAliasJobMap.get(jobMeta.getJobAlias())
      if (jobOpt.isEmpty) {
        wsActor ! ErrorActorEvent(s"Can't find job implementation of ${jobMeta.getJobAlias()}")
        wsActor ! PoisonPill
      } else {
        val job = jobOpt.get
        val (isOk, errMsg) = job.checkJobData(jobData)
        if (isOk) {
          JobExecDesc.from(null, jobMeta, jobData, JobReport.TYPE_TEST, null, user).map(jobExecDesc => {
            job.doTestAsync(jobExecDesc, logMsg => {
              wsActor ! NotifyActorEvent(logMsg)
            }).pipeTo(self)
          }).recover {
            case t: Throwable =>
              self ! Status.Failure(t)
          }
        } else {
          wsActor ! ErrorActorEvent(errMsg)
          wsActor ! PoisonPill
        }
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      JobReportService.indexReport(execDesc.reportId, report).map { res =>
        val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${res.id}"
        wsActor ! NotifyActorEvent(reportUrl)
        wsActor ! OverActorEvent(report)
        wsActor ! PoisonPill
      }.recover {
        case t: Throwable =>
          self ! Status.Failure(t)
      }
    case eventMessage: ActorEvent =>
      wsActor ! eventMessage
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      log.warning(errLog)
      wsActor ! ErrorActorEvent(errLog)
      wsActor ! PoisonPill
  }

  override def postStop(): Unit = {
    log.debug(s"${self.path} is stopped")
  }
}

object JobTestActor {

  def props(user: String, out: ActorRef = null) = Props(new JobTestActor(user, out))

  case class JobTestMessage(jobMeta: JobMeta, jobData: JobData)

}
