package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.TestCase
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.cs.assertion.Assertions
import asura.core.cs.model.BatchOperation.{BatchOperationLabels, BatchTransfer}
import asura.core.cs.model.{AggsQuery, QueryCase, SearchAfterCase}
import asura.core.cs.{CaseContext, CaseRunner}
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Case, FieldKeys}
import asura.core.es.service._
import asura.core.util.{JacksonSupport, JsonPathUtils}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CaseApi @Inject()(implicit system: ActorSystem,
                        val exec: ExecutionContext,
                        val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    CaseService.getById(id).flatMap(response => {
      if (response.isSuccess) {
        if (response.result.nonEmpty) {
          val hit = response.result.hits.hits(0)
          val creator = hit.sourceAsMap.getOrElse(FieldKeys.FIELD_CREATOR, StringUtils.EMPTY).asInstanceOf[String]
          if (StringUtils.isNotEmpty(creator)) {
            UserProfileService.getProfileById(creator).map(userProfile => {
              OkApiRes(ApiRes(data =
                EsResponse.toSingleApiData(response.result, true) + ("_creator" -> userProfile)
              ))
            })
          } else {
            Future.successful(OkApiRes(ApiRes(data = EsResponse.toSingleApiData(response.result, true))))
          }
        } else {
          Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, id))))
        }
      } else {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(response).name))))
      }
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
            CaseService.deleteDoc(id).toOkResult
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
    val cs = req.bodyAs(classOf[Case])
    val user = getProfileId()
    cs.fillCommonFields(user)
    CaseService.index(cs).map(res => {
      activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_UPDATE_CASE, res.id)
      toActionResultFromAny(res)
    })
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    val user = getProfileId()
    activityActor ! Activity(cs.group, cs.project, user, Activity.TYPE_UPDATE_CASE, id)
    CaseService.updateCs(id, cs).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    CaseService.queryCase(queryCase).toOkResult
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
    CaseRunner.test(testCase.id, cs, CaseContext(options = options)).toOkResult
  }

  def getAllAssertions() = Action {
    OkApiRes(ApiRes(data = Assertions.getAll()))
  }

  def searchAfter() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[SearchAfterCase])
    if (query.onlyMe) {
      query.creator = getProfileId()
    }
    CaseService.searchAfter(query).toOkResult
  }

  def aggs() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    CaseService.aroundAggs(aggs).toOkResult
  }

  def trend(groups: Boolean = true) = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    val res = for {
      groups <- if (groups) GroupService.getMaxGroups() else Future.successful(Nil)
      trends <- CaseService.trend(aggs)
    } yield (groups, trends)
    res.map(tuple => {
      OkApiRes(ApiRes(data = Map("groups" -> tuple._1, "trends" -> tuple._2)))
    })
  }

  def aggsLabels(label: String) = Action(parse.byteString).async { implicit req =>
    CaseService.aggsLabels(label).toOkResult
  }

  def batchLabels() = Action(parse.byteString).async { implicit req =>
    val ops = req.bodyAs(classOf[BatchOperationLabels])
    CaseService.batchUpdateLabels(ops).toOkResult
  }

  def batchTransfer() = Action(parse.byteString).async { implicit req =>
    val op = req.bodyAs(classOf[BatchTransfer])
    CaseService.batchTransfer(op).toOkResult
  }

}
