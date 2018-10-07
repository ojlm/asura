package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.BaseApi.OkApiRes
import asura.app.api.model.UserProfile
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, BaseIndex, UserProfile => EsUserProfile}
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
                         implicit val system: ActorSystem,
                         val exec: ExecutionContext,
                         val sessionStore: PlaySessionStore,
                         val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def login() = Action.async { implicit request =>
    val webContext = new PlayWebContext(request, sessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profile = profileManager.get(true)
    if (!profile.isPresent) {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_EmptyProfile))))
    } else {
      val commonProfile = profile.get()
      val token = commonProfile.getAttribute("token")
      val email = commonProfile.getAttribute("mail")
      if (null == token) {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_TokenGeneratedError))))
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
                  activityActor ! Activity(StringUtils.EMPTY, StringUtils.EMPTY, username, Activity.TYPE_NEW_USER, username)
                  if (StringUtils.isNotEmpty(indexResponse.id)) {
                    OkApiRes(ApiRes(data = apiUserProfile))
                  } else {
                    OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_FailToCreateUser)))
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

  def get() = Action(parse.byteString).async { implicit req =>
    UserProfileService.getProfileById(getProfileId()).toOkResult
  }

  def update() = Action(parse.byteString).async { implicit req =>
    val userProfile = req.bodyAs(classOf[EsUserProfile])
    UserProfileService.updateProfile(userProfile).toOkResult
  }
}
