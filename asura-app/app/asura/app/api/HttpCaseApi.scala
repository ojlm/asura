package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.TestCase
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.assertion.Assertions
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, HttpCaseRequest}
import asura.core.es.service._
import asura.core.http.HttpRunner
import asura.core.model.BatchOperation.{BatchOperationLabels, BatchTransfer}
import asura.core.model.{AggsQuery, QueryCase, SearchAfterCase}
import asura.core.runtime.RuntimeContext
import asura.core.util.{JacksonSupport, JsonPathUtils}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HttpCaseApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    HttpCaseRequestService.getById(id).flatMap(response => {
      withSingleUserProfile(id, response)
    })
  }

  def delete(id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    val caseIds = Seq(id)
    val res = for {
      s <- ScenarioService.containCase(caseIds)
      j <- JobService.containCase(caseIds)
    } yield (s, j)
    res.flatMap(resTuple => {
      val (scenarioRes, jobRes) = resTuple
      if (scenarioRes.isSuccess && jobRes.isSuccess) {
        if (preview.nonEmpty && preview.get) {
          Future.successful(toActionResultFromAny(Map(
            "scenario" -> EsResponse.toApiData(scenarioRes.result),
            "job" -> EsResponse.toApiData(jobRes.result)
          )))
        } else {
          if (scenarioRes.result.isEmpty && jobRes.result.isEmpty) {
            HttpCaseRequestService.deleteDoc(id).toOkResult
          } else {
            Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteCase))))
          }
        }
      } else {
        val errorRes = if (!scenarioRes.isSuccess) scenarioRes else jobRes
        ErrorMessages.error_EsRequestFail(errorRes).toFutureFail
      }
    })
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[HttpCaseRequest])
    val user = getProfileId()
    cs.fillCommonFields(user)
    HttpCaseRequestService.index(cs).map(res => {
      activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_UPDATE_CASE, res.id)
      toActionResultFromAny(res)
    })
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[HttpCaseRequest])
    val user = getProfileId()
    activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_UPDATE_CASE, id)
    HttpCaseRequestService.updateCs(id, cs).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    HttpCaseRequestService.queryCase(queryCase).toOkResult
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val testCase = req.bodyAs(classOf[TestCase])
    val cs = testCase.cs
    val user = getProfileId()
    activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_TEST_CASE, StringUtils.notEmptyElse(testCase.id, StringUtils.EMPTY))
    val options = testCase.options
    val initCtx = if (null != options && null != options.initCtx) {
      // make sure use java types, ugly (;
      JsonPathUtils.parse(JacksonSupport.stringify(options.initCtx)).asInstanceOf[java.util.Map[Any, Any]]
    } else {
      null
    }
    options.initCtx = initCtx
    HttpRunner.test(testCase.id, cs, RuntimeContext(options = options)).toOkResult
  }

  def getAllAssertions() = Action {
    OkApiRes(ApiRes(data = Assertions.getAll()))
  }

  def searchAfter() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[SearchAfterCase])
    if (query.onlyMe) {
      query.creator = getProfileId()
    }
    HttpCaseRequestService.searchAfter(query).toOkResult
  }

  def aggs() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    HttpCaseRequestService.aroundAggs(aggs).toOkResult
  }

  def trend(groups: Boolean = true) = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    val res = for {
      groups <- if (groups) GroupService.getMaxGroups() else Future.successful(Nil)
      trends <- HttpCaseRequestService.trend(aggs)
    } yield (groups, trends)
    res.map(tuple => {
      OkApiRes(ApiRes(data = Map("groups" -> tuple._1, "trends" -> tuple._2)))
    })
  }

  def aggsLabels(label: String) = Action(parse.byteString).async { implicit req =>
    HttpCaseRequestService.aggsLabels(HttpCaseRequest.Index, label).toOkResult
  }

  def batchLabels() = Action(parse.byteString).async { implicit req =>
    val ops = req.bodyAs(classOf[BatchOperationLabels])
    HttpCaseRequestService.batchUpdateLabels(ops).toOkResult
  }

  def batchTransfer() = Action(parse.byteString).async { implicit req =>
    val op = req.bodyAs(classOf[BatchTransfer])
    HttpCaseRequestService.batchTransfer(op).toOkResult
  }

}
