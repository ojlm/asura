package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.OpenApiImport
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.{DateUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.api.openapi.{ConvertResults, OpenApiToHttpRequest}
import asura.core.assertion.Assertions
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, HttpCaseRequest, ScenarioStep}
import asura.core.es.service._
import asura.core.http.HttpRunner
import asura.core.http.actor.HttpRunnerActor.TestCaseMessage
import asura.core.model.BatchOperation.{BatchDelete, BatchOperationLabels, BatchTransfer}
import asura.core.model.{AggsQuery, QueryCase, SearchAfterCase}
import asura.core.runtime.RuntimeContext
import asura.core.util.{JacksonSupport, JsonPathUtils}
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HttpCaseApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def cloneRequest(group: String, project: String, id: String) = Action.async { implicit req =>
    HttpCaseRequestService.getRequestById(id).flatMap(httpRequest => {
      httpRequest.copyFrom = id
      httpRequest.fillCommonFields(getProfileId())
      HttpCaseRequestService.index(httpRequest)
    }).toOkResult
  }

  def getById(id: String) = Action.async { implicit req =>
    HttpCaseRequestService.getById(id).flatMap(response => {
      withSingleUserProfile(id, response)
    })
  }

  def delete(id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    deleteDocs(preview, Seq(id))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val httpRequest = req.bodyAs(classOf[HttpCaseRequest])
    val user = getProfileId()
    httpRequest.fillCommonFields(user)
    HttpCaseRequestService.index(httpRequest).map(res => {
      activityActor ! Activity(httpRequest.group, httpRequest.project, user, Activity.TYPE_NEW_CASE, res.id)
      toActionResultFromAny(res)
    })
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val httpRequest = req.bodyAs(classOf[HttpCaseRequest])
    val user = getProfileId()
    activityActor ! Activity(httpRequest.group, httpRequest.project, user, Activity.TYPE_UPDATE_CASE, id)
    HttpCaseRequestService.updateCs(id, httpRequest).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    HttpCaseRequestService.queryCase(queryCase).toOkResult
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val testCase = req.bodyAs(classOf[TestCaseMessage])
    val cs = testCase.cs
    val user = getProfileId()
    activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_TEST_CASE, StringUtils.notEmptyElse(testCase.id, StringUtils.EMPTY))
    val options = testCase.options
    if (null != options && null != options.initCtx) {
      // make sure use java types, ugly (;
      val initCtx = JsonPathUtils.parse(JacksonSupport.stringify(options.initCtx)).asInstanceOf[java.util.Map[Any, Any]]
      options.initCtx = initCtx
    }
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

  def batchDelete(group: String, project: String, preview: Option[Boolean]) = Action(parse.byteString).async { implicit req =>
    val op = req.bodyAs(classOf[BatchDelete])
    deleteDocs(preview, op.ids)
  }

  def openApiPreview(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    val option = req.bodyAs(classOf[OpenApiImport])
    if (StringUtils.isNotEmpty(option.url)) {
      OpenApiToHttpRequest.fromUrl(option.url, option.options).map(dealConvertResults)
    } else if (StringUtils.isNotEmpty(option.content)) {
      Future.successful(OpenApiToHttpRequest.fromContent(option.content, option.options)).map(dealConvertResults)
    } else {
      Future.successful(Nil).toOkResult
    }
  }

  def openApiImport(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    val option = req.bodyAs(classOf[OpenApiImport])
    if (null != option.list && option.list.nonEmpty) {
      val username = getProfileId()
      val now = DateUtils.nowDateTime
      option.list.foreach(item => {
        item.group = group
        item.project = project
        item.fillCommonFields(username, now)
      })
      HttpCaseRequestService.index(option.list).map(response => OkApiRes(ApiRes(data = response.count)))
    } else {
      Future.successful(OkApiRes(ApiRes(data = 0)))
    }
  }

  private def dealConvertResults(results: ConvertResults)(implicit request: RequestHeader) = {
    if (null != results.error) {
      OkApiRes(ApiResError(getI18nMessage(results.error.name)))
    } else {
      OkApiRes(ApiRes(data = results.list))
    }
  }

  private def deleteDocs(preview: Option[Boolean], ids: Seq[String])(implicit request: RequestHeader) = {
    val res = for {
      s <- ScenarioService.containSteps(ids, ScenarioStep.TYPE_HTTP)
      j <- JobService.containCase(ids)
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
            HttpCaseRequestService.deleteDoc(ids).toOkResult
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
}
