package asura.app.api

import asura.core.cs.model.QueryApi
import asura.core.es.model.RestApi
import asura.core.es.service.ApiService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class RestApiApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    ApiService.getById(id).toOkResultByEsOneDoc(id)
  }

  def getOne() = Action(parse.byteString).async { implicit req =>
    val api = req.bodyAs(classOf[RestApi])
    ApiService.getOne(api).toOkResultByEsOneDoc(api.generateId())
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val api = req.bodyAs(classOf[RestApi])
    api.fillCommonFields(getProfileId())
    ApiService.index(api).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryApi])
    ApiService.queryApi(query).toOkResultByEsList()
  }
}
