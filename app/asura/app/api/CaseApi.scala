package asura.app.api

import akka.actor.ActorSystem
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.TestCase
import asura.common.model.ApiRes
import asura.common.util.StringUtils
import asura.core.cs.assertion.Assertions
import asura.core.cs.model.{AggsCase, QueryCase, SearchAfterCase}
import asura.core.cs.{CaseContext, CaseRunner}
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Case}
import asura.core.es.service.CaseService
import asura.core.util.{JacksonSupport, JsonPathUtils}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class CaseApi @Inject()(implicit system: ActorSystem,
                        val exec: ExecutionContext,
                        val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    CaseService.getById(id).toOkResultByEsOneDoc(id)
  }

  def delete(id: String) = Action.async { implicit req =>
    CaseService.deleteDoc(id).toOkResult
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
    val aggs = req.bodyAs(classOf[AggsCase])
    CaseService.aroundAggs(aggs).toOkResult
  }
}
