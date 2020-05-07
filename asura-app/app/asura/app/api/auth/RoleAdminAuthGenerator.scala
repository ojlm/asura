package asura.app.api.auth

import java.util.Optional

import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.UserProfile

case class RoleAdminAuthGenerator(administrators: Seq[String]) extends AuthorizationGenerator {

  override def generate(context: WebContext, profile: UserProfile): Optional[UserProfile] = {
    if (administrators.contains(profile.getId)) {
      profile.addRole(Role.ADMIN)
    }
    Optional.of(profile)
  }
}
