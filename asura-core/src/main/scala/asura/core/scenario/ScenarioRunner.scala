package asura.core.scenario

import akka.actor.ActorRef
import asura.common.actor.{ActorEvent, ItemActorEvent}
import asura.common.util.{LogUtils, StringUtils, XtermUtils}
import asura.core.ErrorMessages
import asura.core.assertion.engine.Statistic
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.JobReportData.{JobReportStepItemData, ReportStepItemStatus, ScenarioReportItemData}
import asura.core.es.model.{HttpCaseRequest, JobReportDataHttpItem, Scenario, ScenarioStep}
import asura.core.es.service.{HttpCaseRequestService, ScenarioService}
import asura.core.http.{HttpResult, HttpRunner}
import asura.core.job.actor.JobReportDataItemSaveActor.SaveReportDataHttpItemMessage
import asura.core.job.{JobReportItemResultEvent, JobReportItemStoreDataHelper}
import asura.core.runtime.{ContextOptions, RuntimeContext}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ScenarioRunner {

  val logger = Logger("ScenarioRunner")

  def testScenarios(
                     scenarioIds: Seq[String],
                     log: String => Unit = null,
                     options: ContextOptions = null
                   )(implicit reportId: String, storeActor: ActorRef, jobId: String): Future[Seq[ScenarioReportItemData]] = {
    val scenarioIdMap = scala.collection.mutable.HashMap[String, Scenario]()
    val scenarioIdCaseIdMap = scala.collection.mutable.HashMap[String, Seq[String]]()
    if (null != scenarioIds && scenarioIds.nonEmpty) {
      ScenarioService.getScenariosByIds(scenarioIds).flatMap(list => {
        val caseIds = ArrayBuffer[String]()
        list.foreach(tuple => {
          val (scenarioId, scenario) = tuple
          val scenarioCaseIds = scenario.steps.filter(ScenarioStep.TYPE_HTTP == _.`type`).map(_.id)
          scenarioIdMap += (scenarioId -> scenario)
          caseIds ++= scenarioCaseIds
          scenarioIdCaseIdMap(scenarioId) = scenarioCaseIds
        })
        HttpCaseRequestService.getByIdsAsMap(caseIds)
      }).flatMap(caseIdMap => {
        val scenarioIdCaseMap = scala.collection.mutable.HashMap[String, Seq[(String, HttpCaseRequest)]]()
        scenarioIdCaseIdMap.foreach(tuple => {
          val (scenarioId, caseIds) = tuple
          // if case was deleted the scenario will ignore it
          val cases = ArrayBuffer[(String, HttpCaseRequest)]()
          caseIds.foreach(id => {
            val value = caseIdMap.get(id)
            if (value.nonEmpty) {
              cases.append((id, value.get))
            }
          })
          scenarioIdCaseMap(scenarioId) = cases
        })
        var index = 0
        val jobReportItemsFutures = scenarioIds.map(scenarioId => {
          val cases = scenarioIdCaseMap(scenarioId)
          val scenario = scenarioIdMap(scenarioId)
          val dataStoreHelper = if (null != reportId && null != storeActor) {
            JobReportItemStoreDataHelper(reportId, s"s${index.toString}", storeActor, jobId)
          } else {
            null
          }
          index = index + 1
          test(scenarioId, scenario.summary, cases, log, options)(dataStoreHelper)
        })
        Future.sequence(jobReportItemsFutures)
      })
    } else {
      Future.successful(Nil)
    }
  }

  /**
    * @param scenarioId if this value is null, previous case context should not be put context
    * @param caseTuples (caseId, case)
    */
  def test(
            scenarioId: String,
            summary: String,
            caseTuples: Seq[(String, HttpCaseRequest)],
            log: String => Unit = null,
            options: ContextOptions = null,
            logResult: ActorEvent => Unit = null,
          )(implicit dataStoreHelper: JobReportItemStoreDataHelper = null): Future[ScenarioReportItemData] = {
    if (null != log) log(s"scenario(${summary}): fetch ${caseTuples.length} cases.")
    if (caseTuples.isEmpty) throw ErrorMessages.error_EmptyJobCaseScenarioCount.toException
    val scenarioReportItem = ScenarioReportItemData(scenarioId, summary)
    val caseReportItems = ArrayBuffer[JobReportStepItemData]()
    scenarioReportItem.steps = caseReportItems
    // for `foldLeft` type inference
    val nullCaseReportItem: JobReportStepItemData = null
    // it will be true only in a real scenario
    val failFast = StringUtils.isNotEmpty(scenarioId)
    var isScenarioFailed = false
    val caseContext = RuntimeContext(options = options)
    var caseIndex = 0
    caseTuples.foldLeft(Future.successful(nullCaseReportItem))((prevCaseReportItemFuture, tuple) => {
      val (id, cs) = tuple
      for {
        prevReportItem <- prevCaseReportItemFuture
        currReportItem <- {
          if (null != prevReportItem) { // not the initial value of `foldLeft`
            caseReportItems += prevReportItem
            caseIndex = caseIndex + 1
          }
          ///////////////////////////////
          // generate case report item //
          ///////////////////////////////
          if (failFast && isScenarioFailed) {
            // add skipped test case report item in a scenario
            val item = JobReportStepItemData(id, cs.summary, null, Statistic())
            item.status = ReportStepItemStatus.STATUS_SKIPPED
            if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.yellowWrap(ReportStepItemStatus.STATUS_SKIPPED)}.")
            if (null != logResult) logResult(ItemActorEvent(JobReportItemResultEvent(caseIndex, item.status, null, null)))
            Future.successful(item)
          } else {
            // execute next test case
            HttpRunner.test(id, cs, caseContext)
              .map { caseResult =>
                var itemDataId: String = null
                if (null != dataStoreHelper) {
                  // save item data if the item not skipped or exception happened
                  itemDataId = s"${dataStoreHelper.reportId}_${dataStoreHelper.infix}_${caseIndex}"
                  val dataItem = JobReportDataHttpItem(
                    reportId = dataStoreHelper.reportId,
                    caseId = id,
                    scenarioId = scenarioId,
                    jobId = dataStoreHelper.jobId,
                    metrics = caseResult.metrics,
                    request = caseResult.request,
                    response = caseResult.response,
                    assertions = caseResult.assert,
                    assertionsResult = caseResult.result,
                    generator = StringUtils.notEmptyElse(caseResult.generator, StringUtils.EMPTY)
                  )
                  dataStoreHelper.actorRef ! SaveReportDataHttpItemMessage(itemDataId, dataItem)
                }
                val statis = caseResult.statis
                val item = if (statis.isSuccessful) {
                  if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.greenWrap(ReportStepItemStatus.STATUS_PASS)}.")
                  if (StringUtils.isNotEmpty(scenarioId)) {
                    // when it's a real scenario instead of a plain array of case
                    caseContext.setPrevCurrentData(RuntimeContext.extractCaseSelfContext(caseResult))
                  }
                  JobReportStepItemData.parse(cs.summary, caseResult, itemDataId)
                } else {
                  if (null != log) log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL)}.")
                  isScenarioFailed = true
                  scenarioReportItem.markFail()
                  // fail because of assertions not pass
                  JobReportStepItemData.parse(cs.summary, caseResult, itemDataId)
                }
                if (null != logResult) logResult(ItemActorEvent(JobReportItemResultEvent(caseIndex, item.status, null, caseResult)))
                item
              }
              .recover {
                case t: Throwable => {
                  // fail because of an exception was thrown
                  val errorStack = LogUtils.stackTraceToString(t)
                  logger.warn(errorStack)
                  if (null != log) {
                    log(s"scenario(${summary}): ${cs.summary} ${XtermUtils.redWrap(ReportStepItemStatus.STATUS_FAIL)}.")
                    log(s"scenario(${summary}): ${cs.summary} error : ${errorStack}.")
                  }
                  val item = JobReportStepItemData.parse(cs.summary, HttpResult.failResult(id), msg = errorStack)
                  scenarioReportItem.markFail()
                  if (null != logResult) logResult(ItemActorEvent(JobReportItemResultEvent(caseIndex, item.status, errorStack, null)))
                  item
                }
              }
          }
        }
      } yield currReportItem
    }).map(lastReportItem => {
      // last report item
      caseReportItems += lastReportItem
      if (StringUtils.isNotEmpty(scenarioId)) {
        // not in real scenario
        if (null != log) log(s"scenario(${summary}): ${
          if (scenarioReportItem.isSuccessful())
            XtermUtils.greenWrap(scenarioReportItem.status)
          else
            XtermUtils.redWrap(scenarioReportItem.status)
        }")
      }
      scenarioReportItem
    })
  }
}
