package asura.app.modules

import asura.app.api.actions.SecurityHttpActionAdapter
import asura.app.api.auth.{LdapAuthenticator, SimpleTestUsernamePasswordAuthenticator}
import com.google.inject.{AbstractModule, Provides}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.{DirectBasicAuthClient, DirectFormClient, HeaderClient}
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.LogoutController
import org.pac4j.play.scala.{DefaultSecurityComponents, SecurityComponents}
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{Configuration, Environment}

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])
    // logout
    val logoutController = new LogoutController()
    logoutController.setDestroySession(true)
    logoutController.setLocalLogout(true)
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
    // security components used in controllers
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])
  }

  @Provides
  def directFormClient: DirectFormClient = {
    if (configuration.getOptional[Boolean]("asura.ldap.enabled").getOrElse(false)) {
      new DirectFormClient(LdapAuthenticator(configuration))
    } else {
      new DirectFormClient(new SimpleTestUsernamePasswordAuthenticator(configuration))
    }
  }

  @Provides
  def directBasicAuthClient: DirectBasicAuthClient = {
    if (configuration.getOptional[Boolean]("asura.ldap.enabled").getOrElse(false)) {
      new DirectBasicAuthClient(LdapAuthenticator(configuration))
    } else {
      new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(configuration))
    }
  }

  @Provides
  def headerClient: HeaderClient = {
    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(configuration.get[String]("asura.jwt.secret")))
    new HeaderClient("Authorization", "Bearer ", jwtAuthenticator)
  }

  @Provides
  def provideConfig(directFormClient: DirectFormClient, headerClient: HeaderClient, directBasicAuthClient: DirectBasicAuthClient): Config = {
    val clients = new Clients(directFormClient, headerClient, directBasicAuthClient)
    val config = new Config(clients)
    config.setHttpActionAdapter(new SecurityHttpActionAdapter())
    // config.addAuthorizer("login", new LoginAuthorizer())
    config
  }
}
