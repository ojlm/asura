package asura.core.job

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat

import akka.actor.{ActorRef, PoisonPill}
import asura.common.util.{DateUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.ContextOptions
import asura.core.es.model.{BaseIndex, Job, JobData, JobReport}
import asura.core.es.service.JobReportService
import asura.core.job.actor.JobReportDataItemSaveActor

import scala.concurrent.Future

/** job meta data and status container during execution */
case class JobExecDesc(
                        val jobId: String,
                        val job: Job,
                        val report: JobReport,
                        val startNano: Long,
                        val configWorkDir: String,
                        val reportId: String,
                        val reportItemSaveActor: ActorRef,
                        val options: ContextOptions = null
                      ) {

  private var jobWorkDir: String = null
  private var stdLogWriter: PrintWriter = null
  private var currentJobFolder: String = null

  def prepareEnd(): JobExecDesc = {
    if (null != reportItemSaveActor) reportItemSaveActor ! PoisonPill
    this.report.endAt = DateUtils.nowDateTime
    this.report.elapse = ((System.nanoTime() - this.startNano) / 1000000).toInt
    if (null != stdLogWriter) {
      stdLogWriter.flush()
      stdLogWriter.close()
    }
    this
  }

  def status(status: String): JobExecDesc = {
    this.report.result = status
    this
  }

  def isSuccess: Boolean = JobExecDesc.STATUS_SUCCESS == this.report.result

  def errorMsg(msg: String): JobExecDesc = {
    this.report.errorMsg = msg
    this
  }

  def workDir = {
    if (null == jobWorkDir) {
      val pattern = new SimpleDateFormat("yyyyMMddHHmmss")
      currentJobFolder = pattern.format(new java.util.Date())
      jobWorkDir = s"$configWorkDir/${job.group}/${job.project}/${jobId}/$currentJobFolder"
      val file = new File(jobWorkDir)
      if (!file.exists()) {
        file.mkdirs()
      }
    }
    jobWorkDir
  }

  def currentJobFolderName = {
    if (null == jobWorkDir) {
      workDir
    }
    currentJobFolder
  }

  def stdWriter = {
    if (null == stdLogWriter) {
      stdLogWriter = new PrintWriter(new BufferedWriter(new FileWriter(s"$workDir/${JobCenter.jobStdLogFileName}")))
    }
    stdLogWriter
  }
}


object JobExecDesc {

  val STATUS_SUCCESS = "success"
  val STATUS_FAIL = "fail"

  def from(jobId: String, job: Job, `type`: String, options: ContextOptions, creator: String): Future[JobExecDesc] = {
    val report = JobReport(
      scheduler = job.scheduler,
      group = job.group,
      project = job.project,
      jobId = jobId,
      jobName = job.summary,
      `type` = `type`,
      classAlias = job.classAlias,
      startAt = DateUtils.nowDateTime
    )
    if (StringUtils.isNotEmpty(creator)) {
      report.fillCommonFields(creator)
    } else {
      report.fillCommonFields(BaseIndex.CREATOR_QUARTZ)
    }
    JobReportService.index(report).map(res => {
      val actor = asura.core.CoreConfig.system.actorOf(JobReportDataItemSaveActor.props(report.data.dayIndexSuffix))
      JobExecDesc(
        jobId = jobId,
        job = job,
        report = report,
        startNano = System.nanoTime(),
        configWorkDir = JobCenter.jobWorkDir,
        reportId = res.id,
        reportItemSaveActor = actor,
        options = options
      )
    })
  }

  def from(jobId: String, jobMeta: JobMeta, jobData: JobData, `type`: String, options: ContextOptions, creator: String): Future[JobExecDesc] = {
    val job = Job(
      summary = jobMeta.summary,
      description = jobMeta.description,
      group = jobMeta.group,
      project = jobMeta.project,
      scheduler = jobMeta.getScheduler(),
      classAlias = jobMeta.getJobAlias(),
      trigger = Nil,
      jobData = jobData
    )
    val report = JobReport(
      scheduler = jobMeta.getScheduler(),
      group = jobMeta.group,
      project = jobMeta.project,
      jobId = jobId,
      jobName = jobMeta.summary,
      `type` = `type`,
      classAlias = jobMeta.getJobAlias(),
      startAt = DateUtils.nowDateTime
    )
    if (StringUtils.isNotEmpty(creator)) {
      report.fillCommonFields(creator)
    } else {
      report.fillCommonFields(BaseIndex.CREATOR_QUARTZ)
    }
    JobReportService.index(report).map(res => {
      val actor = asura.core.CoreConfig.system.actorOf(JobReportDataItemSaveActor.props(report.data.dayIndexSuffix))
      JobExecDesc(
        jobId = jobId,
        job = job,
        report = report,
        startNano = System.nanoTime(),
        configWorkDir = JobCenter.jobWorkDir,
        reportId = res.id,
        reportItemSaveActor = actor,
        options = options
      )
    })
  }
}
