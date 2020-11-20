package asura.app.api

import akka.actor.ActorSystem
import asura.app.AppErrorMessages
import asura.app.api.model.UserProfile
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, BaseIndex, UserProfile => EsUserProfile}
import asura.core.es.service.UserProfileService
import asura.core.model.QueryUser
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.ldap.profile.LdapProfile
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.SecurityComponents
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserApi @Inject()(
                         implicit val system: ActorSystem,
                         val exec: ExecutionContext,
                         val configuration: Configuration,
                         val sessionStore: PlaySessionStore,
                         val controllerComponents: SecurityComponents,
                         val permissionAuthProvider: PermissionAuthProvider,
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
          email = emailStr,
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
              activityActor ! Activity(StringUtils.EMPTY, StringUtils.EMPTY, username, Activity.TYPE_USER_LOGIN, username)
              Future.successful(OkApiRes(ApiRes(data = apiUserProfile)))
            } else {
              // new user
              val esUserProfile = EsUserProfile(
                username = username,
                email = emailStr
              )
              if (commonProfile.isInstanceOf[LdapProfile]) {
                // first time login by ldap
                esUserProfile.fillCommonFields(BaseIndex.CREATOR_LDAP)
              } else {
                // not by ldap
                esUserProfile.fillCommonFields(BaseIndex.CREATOR_STANDARD)
              }
              UserProfileService.index(esUserProfile).map(indexResponse => {
                activityActor ! Activity(StringUtils.EMPTY, StringUtils.EMPTY, username, Activity.TYPE_NEW_USER, username)
                if (StringUtils.isNotEmpty(indexResponse.id)) {
                  OkApiRes(ApiRes(data = apiUserProfile))
                } else {
                  OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_FailToCreateUser)))
                }
              })
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
    userProfile.username = getProfileId()
    UserProfileService.updateDoc(userProfile).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val q = req.bodyAs(classOf[QueryUser])
    UserProfileService.queryDoc(q).toOkResultByEsList(false)
  }

  def isAdmin() = Action(parse.byteString).async { implicit req =>
    permissionAuthProvider.isAdmin(getProfileId()).toOkResult
  }
}
