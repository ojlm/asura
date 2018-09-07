package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.Dtabs
import asura.common.model.ApiRes
import asura.namerd.api.v1.NamerdV1Api
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class LinkerdApi @Inject()(
                            implicit exec: ExecutionContext,
                            val controllerComponents: SecurityComponents,
                            config: Configuration
                          ) extends BaseApi {

  implicit val httpEngine = asura.core.http.HttpEngine.http

  def getHttp() = Action.async { implicit req =>
    NamerdV1Api.getNamespaceDtabs(config.get[String]("asura.linkerd.httpNs")).toOkResult
  }

  def putHttp() = Action(parse.byteString).async { implicit req =>
    val dtabs = req.bodyAs(classOf[Dtabs])
    NamerdV1Api.updateNamespaceDtabs(config.get[String]("asura.linkerd.httpNs"), dtabs.dtabs).toOkResult
  }
}
