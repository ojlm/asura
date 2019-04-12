package asura.gatling.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import asura.cluster.ClusterManager
import asura.cluster.actor.MemberListenerActor.GetAllMembers
import asura.common.model.ApiRes
import asura.common.util.JsonUtils
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc.{Result, _}

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
    Ok("asura-gatling")
  }

  def getClusterMembers() = Action.async {
    (ClusterManager.clusterManagerActor ? GetAllMembers).map(members => {
      Result(
        header = ResponseHeader(200),
        HttpEntity.Strict(ByteString(JsonUtils.stringify(ApiRes(data = members))), Some(ContentTypes.JSON))
      )
    })
  }
}
