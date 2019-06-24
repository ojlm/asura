package asura.pea.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaManagerActor
import asura.pea.actor.PeaManagerActor.{GetNodeStatusMessage, SingleHttpScenarioMessage}
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class GatlingApi @Inject()(
                            implicit val system: ActorSystem,
                            implicit val exec: ExecutionContext,
                            implicit val mat: Materializer,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi {

  val peaManager = system.actorOf(PeaManagerActor.props())

  def status() = Action.async { implicit req =>
    (peaManager ? GetNodeStatusMessage).toOkResult
  }

  def single() = Action(parse.byteString).async { implicit req =>
    val message = req.bodyAs(classOf[SingleHttpScenarioMessage])
    (peaManager ? message).toOkResult
  }
}
