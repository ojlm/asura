package asura.core.job.actor

import akka.actor.{ActorRef, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor._
import asura.common.util.{LogUtils, StringUtils, XtermUtils}
import asura.core.CoreConfig
import asura.core.es.model.{JobData, JobReport, VariablesImportItem}
import asura.core.es.service.JobReportService
import asura.core.job.actor.JobTestActor.{JobOverEvent, JobTestMessage}
import asura.core.job.{JobCenter, JobExecDesc, JobMeta}
import asura.core.runtime.{ContextOptions, ControllerOptions, DebugOptions, RuntimeContext}

class JobTestActor(user: String, out: ActorRef) extends BaseActor {

  implicit val ec = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_JOB_TIMEOUT
  if (null != out) self ! SenderMessage(out)
  var jobId: String = null
  var jobMeta: JobMeta = null
  var jobData: JobData = null
  var imports: Seq[VariablesImportItem] = null
  var controller: ControllerOptions = null
  var debug: DebugOptions = null
  var runtimeContext = RuntimeContext()
  var times = 1 // run times
  var currentTime = 1 // current time order

  override def receive: Receive = {
    case SenderMessage(sender) =>
      context.become(handleRequest(sender))
  }

  def handleRequest(wsActor: ActorRef): Receive = {
    case JobTestMessage(jobId, jobMeta, jobData, imports, controller, debug) =>
      val jobOpt = JobCenter.classAliasJobMap.get(jobMeta.getJobAlias())
      if (jobOpt.isEmpty) {
        wsActor ! ErrorActorEvent(s"Can't find job implementation of ${jobMeta.getJobAlias()}")
        wsActor ! Status.Success
      } else {
        val job = jobOpt.get
        val (isOk, errMsg) = job.checkJobData(jobData)
        if (isOk) {
          this.jobId = jobId
          this.jobMeta = jobMeta
          this.jobData = jobData
          this.imports = imports
          this.controller = controller
          if (null != debug && debug.times > 0) this.times = debug.times
          self ! this.currentTime
        } else {
          wsActor ! ErrorActorEvent(errMsg)
          wsActor ! Status.Success
        }
      }
    case time: Int =>
      if (this.times > 1) {
        val timeMsg = s"Times ${time} start"
        wsActor ! NotifyActorEvent(s"\n[JOB][${this.jobMeta.summary}]: ${XtermUtils.blueWrap(timeMsg)}")
      }
      JobExecDesc.from(
        jobId,
        jobMeta,
        jobData,
        JobReport.TYPE_TEST,
        ContextOptions(jobEnv = jobMeta.env),
        user,
        imports
      ).map(jobExecDesc => {
        jobExecDesc.overrideRuntime = this.runtimeContext
        val runner = context.actorOf(JobRunnerActor.props(wsActor, controller))
        (runner.ask(jobExecDesc)(timeout, self)) pipeTo self
      }).recover {
        case t: Throwable =>
          if (this.times > 1) {
            val timeMsg = s"Times ${time} error"
            wsActor ! NotifyActorEvent(s"\n[JOB][${this.jobMeta.summary}]: ${XtermUtils.redWrap(timeMsg)}")
          }
          self ! Status.Failure(t)
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
        val timeMsg = if (this.times > 1) {
          s"[${XtermUtils.blueWrap(s"Times-${this.currentTime}")}]"
        } else {
          StringUtils.EMPTY
        }
        val reportUrl = s"[REPORT]${timeMsg}: ${CoreConfig.reportBaseUrl}/${report.group}/${report.project}/${res.id}"
        wsActor ! NotifyActorEvent(reportUrl)
        val ctxMap = if (null != execDesc.overrideRuntime) {
          execDesc.overrideRuntime.eraseCurrentData()
          execDesc.overrideRuntime.rawContext
        } else {
          null
        }
        wsActor ! OverActorEvent(JobOverEvent(report, ctxMap))
        this.currentTime = this.currentTime + 1
        if (this.currentTime > this.times || !report.isSuccessful()) {
          wsActor ! Status.Success
        } else {
          self ! this.currentTime
        }
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

  case class JobTestMessage(
                             jobId: String,
                             jobMeta: JobMeta,
                             jobData: JobData,
                             imports: Seq[VariablesImportItem],
                             controller: ControllerOptions,
                             debug: DebugOptions,
                           )

  case class JobOverEvent(report: JobReport, context: java.util.Map[Any, Any])

}
