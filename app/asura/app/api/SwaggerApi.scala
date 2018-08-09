package asura.app.api

import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

@Singleton
class SwaggerApi @Inject()(val controllerComponents: SecurityComponents) extends BaseApi {

  def home() = Action {
    Redirect("/docs/swagger-ui/index.html?url=/assets/swagger.json", MOVED_PERMANENTLY)
  }
}
