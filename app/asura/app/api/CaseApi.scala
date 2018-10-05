package asura.app.api

import akka.actor.ActorSystem
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.TestCase
import asura.common.model.ApiRes
import asura.common.util.StringUtils
import asura.core.cs.CaseRunner
import asura.core.cs.assertion.Assertions
import asura.core.cs.model.QueryCase
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Case}
import asura.core.es.service.CaseService
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
    CaseRunner.test(testCase.id, cs).toOkResult
  }

  def getAllAssertions() = Action {
    OkApiRes(ApiRes(data = Assertions.getAll()))
  }
}
