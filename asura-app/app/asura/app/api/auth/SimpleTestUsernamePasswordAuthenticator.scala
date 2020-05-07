package asura.app.api.auth

import java.time.{LocalDate, ZoneId}
import java.util.Date

import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.CredentialsException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.util.{CommonHelper, Pac4jConstants}
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import play.api.Configuration

class SimpleTestUsernamePasswordAuthenticator(configuration: Configuration) extends Authenticator[UsernamePasswordCredentials] {

  override def validate(credentials: UsernamePasswordCredentials, context: WebContext): Unit = {
    if (credentials == null) throw new CredentialsException("No credential")
    val username = credentials.getUsername
    val password = credentials.getPassword
    if (CommonHelper.isBlank(username)) throw new CredentialsException("Username cannot be blank")
    if (CommonHelper.isBlank(password)) throw new CredentialsException("Password cannot be blank")
    if (CommonHelper.areNotEquals(username, password)) throw new CredentialsException("Username : '" + username + "' does not match password")
    val profile = new CommonProfile()
    profile.setId(username)
    profile.addAttribute(Pac4jConstants.USERNAME, username)
    val jwtGenerator = new JwtGenerator[CommonProfile](new SecretSignatureConfiguration(configuration.get[String]("asura.jwt.secret")))
    val tomorrow = LocalDate.now().plusDays(1).atStartOfDay()
    jwtGenerator.setExpirationTime(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()))
    val token = jwtGenerator.generate(profile)
    profile.addAttribute("token", token)
    credentials.setUserProfile(profile)
  }
}
