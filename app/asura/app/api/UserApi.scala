package asura.app.api

import asura.common.util.JsonUtils
import javax.inject.{Inject, Singleton}
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.SecurityComponents
import org.pac4j.play.store.PlaySessionStore

@Singleton
class UserApi @Inject()(sessionStore: PlaySessionStore, val controllerComponents: SecurityComponents) extends BaseApi {

  def login() = Action { request =>
    val webContext = new PlayWebContext(request, sessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profile = profileManager.get(true)
    Ok(JsonUtils.stringify(if (profile.isPresent) profile.get() else profile))
  }
}
