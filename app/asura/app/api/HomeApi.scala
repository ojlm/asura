package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.common.util.StringUtils
import asura.core.cs.model.QueryHome
import asura.core.es.model._
import asura.core.es.service.HomeService
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
      case Case.Index => "case"
      case Job.Index => "job"
      case _ => StringUtils.EMPTY
    }
  }
}
