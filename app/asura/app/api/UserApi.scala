package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.UserProfile
import asura.common.model.{ApiRes, ApiResError}
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
    if (!profile.isPresent) {
      OkApiRes(ApiResError("Profile is not present"))
    } else {
      val commonProfile = profile.get()
      val token = commonProfile.getAttribute("token")
      val mail = commonProfile.getAttribute("mail")
      if (null == token) {
        OkApiRes(ApiResError("Token is not generated"))
      } else {
        val userProfile = UserProfile(
          username = commonProfile.getId,
          email = if (null != mail) mail.toString else "",
          token = token.toString
        )
        OkApiRes(ApiRes(data = userProfile))
      }
    }
  }
}
