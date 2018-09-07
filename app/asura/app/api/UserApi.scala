package asura.app.api

import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.UserProfile
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.{BaseIndex, UserProfile => EsUserProfile}
import asura.core.es.service.UserProfileService
import javax.inject.{Inject, Singleton}
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.ldap.profile.LdapProfile
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.SecurityComponents
import org.pac4j.play.store.PlaySessionStore

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserApi @Inject()(
                         implicit exec: ExecutionContext,
                         val sessionStore: PlaySessionStore,
                         val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  def login() = Action.async { implicit request =>
    val webContext = new PlayWebContext(request, sessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profile = profileManager.get(true)
    if (!profile.isPresent) {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_EmptyProfile.name))))
    } else {
      val commonProfile = profile.get()
      val token = commonProfile.getAttribute("token")
      val email = commonProfile.getAttribute("mail")
      if (null == token) {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_TokenGeneratedError.name))))
      } else {
        val username = commonProfile.getId
        val emailStr = if (null != email) email.toString else StringUtils.EMPTY
        val apiUserProfile = UserProfile(
          token = token.toString,
          username = username,
          email = emailStr
        )
        UserProfileService.getProfileById(username)
          .flatMap(profile => {
            if (null != profile) {
              // already registered
              apiUserProfile.nickname = profile.nickname
              apiUserProfile.avatar = profile.avatar
              apiUserProfile.summary = profile.summary
              apiUserProfile.description = profile.description
              apiUserProfile.email = profile.email
              Future.successful(OkApiRes(ApiRes(data = apiUserProfile)))
            } else {
              if (commonProfile.isInstanceOf[LdapProfile]) {
                // first time login by ldap
                val esUserProfile = EsUserProfile(
                  username = username,
                  email = emailStr
                )
                esUserProfile.fillCommonFields(BaseIndex.CREATOR_LDAP)
                UserProfileService.index(esUserProfile).map(indexResponse => {
                  if (StringUtils.isNotEmpty(indexResponse.id)) {
                    OkApiRes(ApiRes(data = apiUserProfile))
                  } else {
                    OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_FailToCreateUser.name)))
                  }
                })
              } else {
                // code should not run here
                Future.successful(OkApiRes(ApiResError(ErrorMessages.error_ServerError.name)))
              }
            }
          })
      }
    }
  }
}
