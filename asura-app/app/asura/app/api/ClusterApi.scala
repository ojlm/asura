package asura.app.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.cluster.ClusterManager
import asura.cluster.actor.ClusterManagerActor.GetAllMembers
import asura.common.model.ApiResError
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClusterApi @Inject()(
                            implicit val system: ActorSystem,
                            val exec: ExecutionContext,
                            val configuration: Configuration,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi {

  implicit val timeout: Timeout = 30.seconds

  def getMembers() = Action(parse.byteString).async { implicit req =>
    checkClusterEnabled {
      (ClusterManager.clusterManagerActor ? GetAllMembers).toOkResult
    }
  }

  private def checkClusterEnabled(func: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (ClusterManager.enabled) {
      func
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_ClusterNotEnabled))))
    }
  }
}
