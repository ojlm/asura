package asura.gatling.api

import javax.inject._
import play.api.mvc._

@Singleton
class HomeApi @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("hello asura-gatling")
  }
}
