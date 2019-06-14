package asura.pea.api

import akka.actor.ActorSystem
import akka.util.Timeout
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class HomeApi @Inject()(
                         implicit val system: ActorSystem,
                         val exec: ExecutionContext,
                         val configuration: Configuration,
                         val cc: ControllerComponents,
                       ) extends AbstractController(cc) {

  implicit val timeout: Timeout = 30.seconds

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("asura-pea")
  }
}
