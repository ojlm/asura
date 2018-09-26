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
import com.typesafe.scalalogging.Logger

class JobTestActor(user: String, out: ActorRef) extends BaseActor {

  import JobTestActor.logger

  implicit val executionContext = context.dispatcher
  if (null != out) self ! SenderMessage(out)

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(webActor: ActorRef): Receive = {
    case JobTestMessage(jobMeta, jobData) =>
      val jobOpt = JobCenter.classAliasJobMap.get(jobMeta.getJobAlias())
      if (jobOpt.isEmpty) {
        webActor ! ErrorActorEvent(s"Can't find job implementation of ${jobMeta.getJobAlias()}")
        webActor ! PoisonPill
      } else {
        val job = jobOpt.get
        val (isOk, errMsg) = job.checkJobData(jobData)
        if (isOk) {
          job.doTestAsync(JobExecDesc.from(null, jobMeta, jobData, JobReport.TYPE_TEST, null), logMsg => {
            webActor ! NotifyActorEvent(logMsg)
          }).pipeTo(self)
        } else {
          webActor ! ErrorActorEvent(errMsg)
          webActor ! PoisonPill
        }
      }
    case execDesc: JobExecDesc =>
      execDesc.prepareEnd()
      val report = execDesc.report
      report.fillCommonFields(user)
      JobReportService.index(report).map { res =>
        logger.debug(s"job(${execDesc.job.summary}) report(${res.id}) is saved.")
        val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${res.id}"
        webActor ! NotifyActorEvent(reportUrl)
        webActor ! ItemActorEvent(execDesc)
        webActor ! PoisonPill
      }
    case eventMessage: ActorEvent =>
      webActor ! eventMessage
    case Status.Failure(t) =>
      val errLog = LogUtils.stackTraceToString(t)
      logger.warn(errLog)
      webActor ! ErrorActorEvent(errLog)
      webActor ! PoisonPill
  }

  override def postStop(): Unit = {
    logger.debug(s"${self.path} is stopped")
  }
}

object JobTestActor {

  def props(user: String, out: ActorRef = null) = Props(new JobTestActor(user, out))

  val logger = Logger(classOf[JobStatusActor])

  case class JobTestMessage(jobMeta: JobMeta, jobData: JobData)

}
