package asura.app.api.auth

import org.pac4j.core.profile.CommonProfile

class TokenProfile() extends CommonProfile {
  var token: String = null
}
