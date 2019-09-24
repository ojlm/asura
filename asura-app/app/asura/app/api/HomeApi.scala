package asura.app.api

import asura.common.model.ApiRes
import asura.core.es.model._
import asura.core.es.service.HomeService
import asura.core.model.QueryHome
import asura.play.api.BaseApi.OkApiRes
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.http.HttpErrorHandler
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

@Singleton
class HomeApi @Inject()(
                         implicit exec: ExecutionContext,
                         val controllerComponents: SecurityComponents,
                         val assets: Assets,
                         val errorHandler: HttpErrorHandler,
                       )
  extends BaseApi {

  def index() = assets.at("index.html")

  def asset(resource: String): Action[AnyContent] = {
    if (resource.startsWith("api") || resource.startsWith("openapi")) {
      Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
    } else {
      if (resource.contains(".")) assets.at(resource) else index
    }
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryHome = req.bodyAs(classOf[QueryHome])
    HomeService.queryDoc(queryHome).map(res => {
      OkApiRes(ApiRes(data = res.result.hits.hits.map(hit => {
        hit.sourceAsMap ++ Seq(
          (FieldKeys.FIELD__ID -> hit.id),
          ("_type" -> getDocType(hit.index))
        )
      })))
    })
  }

  private def getDocType(index: String): String = {
    index match {
      case Group.Index => "group"
      case Project.Index => "project"
      case RestApi.Index => "rest"
      case HttpCaseRequest.Index => "case"
      case DubboRequest.Index => "dubbo"
      case SqlRequest.Index => "sql"
      case Environment.Index => "env"
      case Scenario.Index => "scenario"
      case Job.Index => "job"
      case _ => index
    }
  }
}
