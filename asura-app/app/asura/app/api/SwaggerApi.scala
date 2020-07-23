package asura.app.api

import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

@Deprecated
@Singleton
class SwaggerApi @Inject()(val controllerComponents: SecurityComponents) extends BaseApi {

  def home() = Action {
    Redirect("/openapi/swagger-ui/index.html?url=/assets/swagger.json", MOVED_PERMANENTLY)
  }

  def editor(url: Option[String]) = Action {
    val param = if (url.nonEmpty) s"?url=${url.get}" else ""
    Redirect(s"/openapi/swagger-editor/index.html${param}", MOVED_PERMANENTLY)
  }
}
