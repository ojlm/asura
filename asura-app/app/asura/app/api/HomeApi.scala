package asura.app.api

import asura.common.model.ApiRes
import asura.core.es.model._
import asura.core.es.service.HomeService
import asura.core.model.QueryHome
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class HomeApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

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
