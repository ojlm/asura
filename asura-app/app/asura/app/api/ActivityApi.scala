package asura.app.api

import akka.actor.ActorSystem
import asura.core.es.model.Permissions.Functions
import asura.core.es.service._
import asura.core.model.SearchAfterActivity
import asura.core.security.PermissionAuthProvider
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ActivityApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents,
                            val permissionAuthProvider: PermissionAuthProvider,
                           ) extends BaseApi {

  def recentProjects(wd: String = null, discover: Boolean = false) = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.ACTIVITY_RECENT_PROJECT) { _ =>
      RecommendService.getRecommendProjects(getProfileId(), wd, discover).toOkResult
    }
  }

  def feedSelf() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.ACTIVITY_FEED_SELF) { user =>
      val query = req.bodyAs(classOf[SearchAfterActivity])
      query.user = user
      ActivityService.searchFeed(query).toOkResult
    }
  }

  def feedAll() = Action(parse.byteString).async { implicit req =>
    checkPermission(null, None, Functions.ACTIVITY_FEED_ALL) { _ =>
      val query = req.bodyAs(classOf[SearchAfterActivity])
      ActivityService.searchFeed(query).toOkResult
    }
  }
}
