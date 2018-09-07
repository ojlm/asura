package asura.core.cs.scenario

import asura.common.util.LogUtils
import asura.core.cs.{CaseContext, CaseResult, CaseRunner, ContextOptions}
import asura.core.es.model.JobReportData.{CaseReportItem, ScenarioReportItem}
import asura.core.es.model.{Case, Scenario}
import asura.core.es.service.{CaseService, ScenarioService}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object ScenarioRunner {

  val logger = Logger("ScenarioRunner")

  def testScenarios(
                     scenarioIds: Seq[String],
                     log: String => Unit = null,
                     options: ContextOptions = null
                   ): Future[Seq[ScenarioReportItem]] = {
    val scenarioIdMap = scala.collection.mutable.HashMap[String, Scenario]()
    val scenarioIdCaseIdMap = scala.collection.mutable.HashMap[String, Seq[String]]()
    if (null != scenarioIds && scenarioIds.nonEmpty) {
      import asura.core.concurrent.ExecutionContextManager.cachedExecutor
      ScenarioService.getScenariosByIds(scenarioIds).flatMap(list => {
        val caseIds = ArrayBuffer[String]()
        list.foreach(tuple => {
          val (scenarioId, scenario) = tuple
          val scenarioCaseIds = scenario.cases.map(_.id)
          scenarioIdMap += (scenarioId -> scenario)
          caseIds ++= scenarioCaseIds
          scenarioIdCaseIdMap(scenarioId) = scenarioCaseIds
        })
        CaseService.getCasesByIdsAsMap(caseIds)
      }).flatMap(caseIdMap => {
        val scenarioIdCaseMap = scala.collection.mutable.HashMap[String, Seq[(String, Case)]]()
        scenarioIdCaseIdMap.foreach(tuple => {
          val (scenarioId, caseIds) = tuple
          // if case was deleted the scenario will ignore it
          val cases = caseIds.filter(id => caseIdMap.contains(id)).map(id => (id, caseIdMap(id)))
          scenarioIdCaseMap(scenarioId) = cases
        })
        val jobReportItemsFutures = scenarioIdCaseMap.map(tuple => {
          val (scenarioId, cases) = tuple
          val scenario = scenarioIdMap(scenarioId)
          test(scenarioId, scenario.summary, cases, log, options)
        })
        Future.sequence(jobReportItemsFutures.toSeq)
      })
    } else {
      Future.successful(Nil)
    }
  }

  // scenarioId, (caseId, case)
  def test(
            scenarioId: String,
            summary: String,
            cases: Seq[(String, Case)],
            log: String => Unit = null,
            options: ContextOptions = null,
          )(implicit executionContext: ExecutionContext): Future[ScenarioReportItem] = {
    if (null != log) log(s"scenario(${summary}): fetch ${cases.length} cases.")
    val scenarioReportItem = ScenarioReportItem(scenarioId, summary)
    val caseReportItems = ArrayBuffer[CaseReportItem]()
    val initialCaseResult: CaseResult = CaseResult(null, null, null, null)
    var isScenarioFailed = false
    val caseContext = CaseContext(options = options)
    val caseIdMap = scala.collection.mutable.Map[String, Case]()
    cases.foldLeft(Future(initialCaseResult))((prevCaseResultFuture, tuple) => {
      val (id, cs) = tuple
      caseIdMap += (id -> cs)
      for {
        prevResult <- prevCaseResultFuture
        currResult <- {
          if (!isScenarioFailed) {
            if (null != prevResult) { // not first
              if (prevResult.statis.isSuccessful) {
                if (null != prevResult.id) {
                  val cs = caseIdMap(prevResult.id)
                  if (null != log) log(s"scenario(${summary}): ${cs.summary} result is ok.")
                  caseReportItems += CaseReportItem.parse(cs.summary, prevResult)
                }
                caseContext.setPrevCurrentData(CaseContext.extractCaseSelfContext(prevResult))
                CaseRunner.test(id, cs, caseContext).recover {
                  case NonFatal(t) =>
                    val cs = caseIdMap(id)
                    val errorStack = LogUtils.stackTraceToString(t)
                    logger.warn(errorStack)
                    if (null != log) log(s"scenario(${summary}): ${cs.summary} error : ${errorStack}.")
                    isScenarioFailed = true
                    scenarioReportItem.markFail(t.getMessage)
                    caseReportItems += CaseReportItem.parse(
                      cs.summary,
                      CaseResult.failResult(id, cs),
                      t.getMessage
                    )
                    null
                }
              } else {
                if (null != log) log(s"scenario(${summary}): ${cs.summary} result is failed.")
                isScenarioFailed = true
                scenarioReportItem.markFail()
                caseReportItems += CaseReportItem.parse(caseIdMap(prevResult.id).summary, prevResult)
                Future.successful(null)
              }
            } else {
              Future.successful(null)
            }
          } else {
            Future.successful(null)
          }
        }
      } yield currResult
    }).map(lastCaseResult => {
      // last case result
      if (null != lastCaseResult) {
        if (lastCaseResult.statis.isSuccessful) {
          if (null != lastCaseResult.id) {
            caseReportItems += CaseReportItem.parse(caseIdMap(lastCaseResult.id).summary, lastCaseResult)
          }
        } else {
          caseReportItems += CaseReportItem.parse(caseIdMap(lastCaseResult.id).summary, lastCaseResult)
          scenarioReportItem.markFail()
        }
      }
      scenarioReportItem.cases = caseReportItems
      if (null != log) log(s"scenario(${summary}): ${scenarioReportItem.msg}")
      scenarioReportItem
    })
  }
}
