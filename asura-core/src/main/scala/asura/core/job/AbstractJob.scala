package asura.core.job

import asura.common.util.FutureUtils.RichFuture
import asura.core.es.model.{BaseIndex, JobReport, Job => AsuraJob}
import asura.core.es.service.{JobReportService, JobService}
import asura.core.job.actor.{JobFinished, JobRunning, SchedulerActor}
import com.typesafe.scalalogging.Logger
import org.quartz.{Job, JobExecutionContext}

/** job execution wrap */
abstract class AbstractJob extends Job {

  val logger = Logger(getClass)
  var jobExecutionContext: JobExecutionContext = null

  override def execute(context: JobExecutionContext) = {
    this.jobExecutionContext = context
    val execDesc = beforeRun(context)
    try {
      val job = execDesc.job
      SchedulerActor.statusMonitor ! JobRunning(job.scheduler, job.group, job.name)
      run(execDesc)
    } catch {
      case t: Throwable =>
        execDesc.status(JobExecDesc.STATUS_FAIL).errorMsg(t.getMessage)
    } finally {
      afterRun(execDesc)
    }
  }

  private def beforeRun(context: JobExecutionContext): JobExecDesc = {
    val scheduler = context.getScheduler.getSchedulerName
    val jobKey = context.getJobDetail.getKey
    val jobId = AsuraJob.buildJobKey(scheduler, jobKey.getGroup, jobKey.getName)
    val job = JobService.geJobById(jobId).await
    JobExecDesc(job, JobReport.TYPE_QUARTZ)
  }

  private def afterRun(jobExecDesc: JobExecDesc): Unit = {
    jobExecDesc.prepareEnd()
    saveAndNotifyJobResult(jobExecDesc)
  }

  private def saveAndNotifyJobResult(execDesc: JobExecDesc): Unit = {
    val job = execDesc.job
    val report = execDesc.report
    report.fillCommonFields(BaseIndex.CREATOR_QUARTZ)
    SchedulerActor.statusMonitor ! JobFinished(job.scheduler, job.group, job.name, execDesc.report)
    JobReportService.index(report).await match {
      case Left(failure) =>
        logger.error(s"save job report fail: ${failure.error.reason}")
      case Right(success) =>
        logger.debug(s"job(${AsuraJob.buildJobKey(execDesc.job)}) report(${success.result.id}) is saved.")
    }
  }

  def pauseSelf(jobExecDesc: JobExecDesc): Unit = {
    this.jobExecutionContext.getScheduler.pauseJob(jobExecutionContext.getJobDetail.getKey)
  }

  /** Main business what job do, any job should implements this method.
    * The default job status is success, you should set job status and error message if
    * the job should not be successful.
    * Other metrics like time cost the job implements need not to care.
    */
  def run(execDesc: JobExecDesc)
}
