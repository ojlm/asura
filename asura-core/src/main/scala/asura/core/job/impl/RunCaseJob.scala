package asura.core.job.impl

import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.FutureUtils.RichFuture
import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.scenario.{ItemStoreDataHelper, ScenarioRunner}
import asura.core.es.model.{HttpCaseRequest, JobData}
import asura.core.es.service.HttpCaseRequestService
import asura.core.job._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object RunCaseJob extends JobBase {

  override val meta = JobMeta(
    group = "",
    project = "",
    env = StringUtils.EMPTY,
    summary = "job for running case",
    description = "job for running case~~",
    classAlias = "RunCaseJob"
  )

  override def checkJobData(jobData: JobData): BoolErrorRes = {
    if (null == jobData || ((null == jobData.cs || jobData.cs.isEmpty)
      && (null == jobData.scenario || jobData.scenario.isEmpty)
      && null == jobData.ext)
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
      ScenarioRunner.testScenarios(scenarios.map(_.id), log, execDesc.options)(execDesc.reportId, execDesc.reportItemSaveActor, execDesc.jobId)
        .map(reportItems => {
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
    if (null != cases && !cases.isEmpty || null != jobData.ext) {
      val scenarioReportFuture = if (null != cases && !cases.isEmpty) {
        val caseIds = cases.map(_.id)
        HttpCaseRequestService.getByIdsAsMap(caseIds, false).flatMap(caseIdMap => {
          val cases = ArrayBuffer[(String, HttpCaseRequest)]()
          caseIds.foreach(id => {
            val value = caseIdMap.get(id)
            if (value.nonEmpty) {
              cases.append((id, value.get))
            }
          })
          if (null != jobData.ext) {
            HttpCaseRequestService.getCasesByJobDataExtAsMap(execDesc.job.group, execDesc.job.project, jobData.ext).flatMap(res => {
              res.foreach(idCsTuple => cases.append((idCsTuple._1, idCsTuple._2)))
              val storeDataHelper = ItemStoreDataHelper(execDesc.reportId, "c", execDesc.reportItemSaveActor, execDesc.jobId)
              ScenarioRunner.test(null, "job cases", cases, log, execDesc.options)(storeDataHelper)
            })
          } else {
            val storeDataHelper = ItemStoreDataHelper(execDesc.reportId, "c", execDesc.reportItemSaveActor, execDesc.jobId)
            ScenarioRunner.test(null, "job cases", cases, log, execDesc.options)(storeDataHelper)
          }
        })
      } else {
        val cases = ArrayBuffer[(String, HttpCaseRequest)]()
        HttpCaseRequestService.getCasesByJobDataExtAsMap(execDesc.job.group, execDesc.job.project, jobData.ext).flatMap(res => {
          res.foreach(idCsTuple => cases.append((idCsTuple._1, idCsTuple._2)))
          val storeDataHelper = ItemStoreDataHelper(execDesc.reportId, "c", execDesc.reportItemSaveActor, execDesc.jobId)
          ScenarioRunner.test(null, "job cases", cases, log, execDesc.options)(storeDataHelper)
        })
      }
      scenarioReportFuture.map(scenarioReport => {
        report.data.cases = scenarioReport.steps
        if (scenarioReport.isFailed()) {
          report.result = JobExecDesc.STATUS_FAIL
        }
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
