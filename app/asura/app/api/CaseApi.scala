package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.cs.CaseRunner
import asura.core.cs.model.QueryCase
import asura.core.es.model.Case
import asura.core.es.service.CaseService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class CaseApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    CaseService.getById(id).map(toActionResultWithSingleData(_, id))
  }

  def delete(id: String) = Action.async { implicit req =>
    CaseService.deleteDoc(id).map(res => OkApiRes(ApiRes(data = res)))
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    cs.fillCommonFields(getProfileId())
    CaseService.index(cs).map(res => OkApiRes(ApiRes(data = res)))
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryCase = req.bodyAs(classOf[QueryCase])
    CaseService.queryCase(queryCase).map(res => OkApiRes(ApiRes(data = res)))
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val cs = req.bodyAs(classOf[Case])
    CaseRunner.test("test", cs).map(res => OkApiRes(ApiRes(data = res)))
  }
}
