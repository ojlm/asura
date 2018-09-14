package asura.core.job.impl

import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.FutureUtils.RichFuture
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.scenario.ScenarioRunner
import asura.core.es.model.JobData
import asura.core.es.service.CaseService
import asura.core.job._

import scala.concurrent.Future

object RunCaseJob extends JobBase {

  override val meta = JobMeta(
    group = "",
    project = "",
    summary = "job for running case",
    description = "job for running case~~",
    classAlias = "RunCaseJob"
  )

  override def checkJobData(jobData: JobData): BoolErrorRes = {
    if (null == jobData ||
      ((null == jobData.cs || jobData.cs.isEmpty) && (null == jobData.scenario || jobData.scenario.isEmpty))
    ) {
      (false, ApiMsg.EMPTY_DATA)
    } else {
      (true, ApiMsg.SUCCESS)
    }
  }

  override def doTest(execDesc: JobExecDesc, log: String => Unit): Unit = {
    doTestAsync(execDesc, log).await
  }

  override def doTestAsync(execDesc: JobExecDesc, log: String => Unit): Future[JobExecDesc] = {
    for {
      _ <- doTestCase(execDesc, log)
      _ <- doTestScenario(execDesc, log)
    } yield execDesc
  }

  def doTestScenario(execDesc: JobExecDesc, log: String => Unit): Future[JobExecDesc] = {
    val scenarios = execDesc.job.jobData.scenario
    if (null != scenarios && !scenarios.isEmpty) {
      val report = execDesc.report
      ScenarioRunner.testScenarios(scenarios.map(_.id), log).map(reportItems => {
        report.data.scenarios = reportItems
        reportItems.foreach(item => {
          if (!item.isSuccessful()) {
            report.result = JobExecDesc.STATUS_FAIL
          }
        })
        execDesc
      })
    } else {
      Future.successful(execDesc)
    }
  }

  def doTestCase(execDesc: JobExecDesc, log: String => Unit): Future[JobExecDesc] = {
    val report = execDesc.report
    val jobData = execDesc.job.jobData
    val cases = jobData.cs
    if (null != cases && !cases.isEmpty) {
      if (null != log) log("start fetch cases...")
      CaseService.getCasesByIds(cases.map(_.id), false).flatMap(cases => {
        if (null != log) log(s"fetch ${cases.length} cases.")
        ScenarioRunner.test(null, "job cases", cases, log, execDesc.options)
      }).map(scenarioReport => {
        val reportItems = scenarioReport.cases
        report.data.cases = reportItems
        reportItems.foreach(reportItem => {
          val result = reportItem.result
          if (null != result) {
            val statis = result.statis
            if (!statis.isSuccessful) {
              report.result = JobExecDesc.STATUS_FAIL
            }
          } else {
            report.result = JobExecDesc.STATUS_WARN
          }
        })
        execDesc
      })
    } else {
      Future.successful(execDesc)
    }
  }
}

class RunCaseJob extends AbstractJob {

  override def run(jobExecDesc: JobExecDesc): Unit = {
    RunCaseJob.doTest(jobExecDesc)
  }
}
