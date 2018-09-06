package asura.core.job.actor

import akka.actor.{ActorRef, PoisonPill, Props, Status}
import akka.pattern.pipe
import asura.common.actor._
import asura.core.CoreConfig
import asura.core.actor.messages.SenderMessage
import asura.core.es.model.{Job, JobData, JobReport}
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
      val jobOpt = JobCenter.classAliasJobMap.get(jobMeta.classAlias)
      if (jobOpt.isEmpty) {
        webActor ! ErrorActorEvent(s"Can't find job implementation of ${jobMeta.classAlias}")
        webActor ! PoisonPill
      } else {
        val job = jobOpt.get
        val (isOk, errMsg) = job.checkJobData(jobData)
        if (isOk) {
          job.doTestAsync(JobExecDesc.from(jobMeta, jobData, JobReport.TYPE_TEST), logMsg => {
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
        res match {
          case Left(failure) =>
            val errMsg = s"save job report fail: ${failure.error.reason}"
            logger.error(errMsg)
            webActor ! NotifyActorEvent(errMsg)
          case Right(success) =>
            logger.debug(s"job(${Job.buildJobKey(execDesc.job)}) report(${success.result.id}) is saved.")
            val reportUrl = s"view report: ${CoreConfig.reportBaseUrl}/${success.result.id}"
            webActor ! NotifyActorEvent(reportUrl)
        }
        webActor ! ItemActorEvent(execDesc)
        webActor ! PoisonPill
      }
    case eventMessage: ActorEvent =>
      webActor ! eventMessage
    case Status.Failure(t) =>
      webActor ! ErrorActorEvent(t.getMessage)
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


