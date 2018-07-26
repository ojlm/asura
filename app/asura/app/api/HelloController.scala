package asura.app.api

import akka.actor.ActorSystem
import asura.app.api.model.Message
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class HelloController @Inject()
(actorSystem: ActorSystem)(implicit exec: ExecutionContext, val controllerComponents: ControllerComponents) extends BaseController {
  implicit val fmt = Json.format[Message]

  def hello: Action[AnyContent] = Action.async {
    getFutureMessage(1.second).map { msg ⇒ Ok(Json.toJson(msg)) }
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[Message] = {
    val promise: Promise[Message] = Promise[Message]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success(Message("Hello!"))
    }
    promise.future
  }

}

