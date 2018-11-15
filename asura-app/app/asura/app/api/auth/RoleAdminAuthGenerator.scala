package asura.app.api.auth

import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile

case class RoleAdminAuthGenerator(administrators: Seq[String]) extends AuthorizationGenerator[CommonProfile] {

  override def generate(context: WebContext, profile: CommonProfile): CommonProfile = {
    if (administrators.contains(profile.getId)) {
      profile.addRole(Role.ADMIN)
    }
    profile
  }
}
