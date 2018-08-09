package asura.app.api.authorizers

import java.util

import org.pac4j.core.authorization.authorizer.ProfileAuthorizer
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile

class LoginAuthorizer extends ProfileAuthorizer[CommonProfile] {

  override def isProfileAuthorized(context: WebContext, profile: CommonProfile): Boolean = {
    if (null != profile) {
      true
    } else {
      false
    }
  }

  override def isAuthorized(context: WebContext, profiles: util.List[CommonProfile]): Boolean = {
    isAnyAuthorized(context, profiles)
  }
}
