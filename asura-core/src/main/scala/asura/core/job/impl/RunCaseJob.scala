package asura.core.job.impl

import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.FutureUtils.RichFuture
import asura.core.cs.scenario.ScenarioRunner
import asura.core.cs.{CaseContext, CaseRunner}
import asura.core.es.model.JobData
import asura.core.es.model.JobReportData.{CaseReportItem, ReportItemStatus}
import asura.core.es.service.CaseService
import asura.core.job._

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal

object RunCaseJob extends JobBase {

  override val meta = JobMeta(
    name = "执行接口测试",
    desc = "执行接口测~~",
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
    import asura.core.concurrent.ExecutionContextManager.cachedExecutor
    for {
      _ <- doTestCase(execDesc, log)
      _ <- doTestScenario(execDesc, log)
    } yield execDesc
  }

  def doTestScenario(execDesc: JobExecDesc, log: String => Unit): Future[JobExecDesc] = {
    val scenarios = execDesc.job.jobData.scenario
    if (null != scenarios && !scenarios.isEmpty) {
      val report = execDesc.report
      import asura.core.concurrent.ExecutionContextManager.cachedExecutor
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
    val reportData = report.data
    val jobData = execDesc.job.jobData
    val cases = jobData.cs
    if (null != cases && !cases.isEmpty) {
      val caseReportMap = mutable.Map[String, CaseReportItem]()
      if (null != log) log("start fetch cases...")
      import asura.core.concurrent.ExecutionContextManager.cachedExecutor
      CaseService.getCasesByIds(cases.map(_.id)).flatMap(cases => {
        if (null != log) log(s"fetch ${cases.length} cases.")
        val futureSeq = cases.map(csWrap => {
          val (id, cs) = (csWrap._1, csWrap._2)
          val reportItem = CaseReportItem(id = id, title = cs.summary)
          caseReportMap += (id -> reportItem)
          if (null != log) log(s"${reportItem.title} => test is starting...")
          CaseRunner.test(id, cs, CaseContext(options = execDesc.options)).map(caseResult => {
            val reportItem = caseReportMap(id)
            val statis = caseResult.statis
            if (null != log) log(s"${reportItem.title} => result:${statis.isSuccessful}")
            if (statis.isSuccessful) {
              reportItem.status = ReportItemStatus.STATUS_SUCCESS
            } else {
              reportItem.status = ReportItemStatus.STATUS_FAIL
            }
            reportItem.result = caseResult
            reportItem
          }).recover {
            case NonFatal(t) =>
              reportItem.msg = t.getMessage
              reportItem.status = ReportItemStatus.STATUS_FAIL
              if (null != log) log(s"${reportItem.title} => ${reportItem.msg}")
              reportItem
          }
        })
        Future.sequence(futureSeq)
      }).map(reportItems => {
        reportData.cases = reportItems
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
