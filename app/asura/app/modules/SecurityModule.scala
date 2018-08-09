package asura.app.modules

import asura.app.api.actions.SecurityHttpActionAdapter
import asura.app.api.auth.SimpleTestUsernamePasswordAuthenticator
import com.google.inject.{AbstractModule, Provides}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.{DirectFormClient, ParameterClient}
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
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
    // security components used in controllers
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])
  }

  @Provides
  def provideDirectFormClient: DirectFormClient = new DirectFormClient(new SimpleTestUsernamePasswordAuthenticator(configuration))

  @Provides
  def provideParameterClient: ParameterClient = {
    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(configuration.get[String]("asura.jwt.secret")))
    val parameterClient = new ParameterClient("token", jwtAuthenticator)
    parameterClient.setSupportGetRequest(true)
    parameterClient.setSupportPostRequest(true)
    parameterClient
  }

  @Provides
  def provideConfig(provideDirectFormClient: DirectFormClient, provideParameterClient: ParameterClient): Config = {
    val clients = new Clients(provideDirectFormClient, provideParameterClient)
    val config = new Config(clients)
    config.setHttpActionAdapter(new SecurityHttpActionAdapter())
    // config.addAuthorizer("login", new LoginAuthorizer())
    config
  }
}
