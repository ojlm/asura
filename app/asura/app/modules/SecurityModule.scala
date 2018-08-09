package asura.app.modules

import asura.app.api.actions.SecurityHttpActionAdapter
import asura.app.api.authorizers.LoginAuthorizer
import com.google.inject.{AbstractModule, Provides}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.DirectFormClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.play.LogoutController
import org.pac4j.play.scala.{DefaultSecurityComponents, SecurityComponents}
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{Configuration, Environment}

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])
    // security components used in controllers
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])
    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)

    // LDAP
    //    val connConfig = new ConnectionConfig()
    //    connConfig.setConnectTimeout(Duration.ofMillis(configuration.get[Long]("ldap.connection-timeout")))
    //    connConfig.setResponseTimeout(Duration.ofMillis(configuration.get[Long]("ldap.response-timeout")))
    //    connConfig.setLdapUrl(configuration.get[String]("ldap.url"))
    //    connConfig.setConnectionInitializer(new BindConnectionInitializer(configuration.get[String]("ldap.bind-dn"), new Credential(configuration.get[String]("ldap.password"))))
    //
    //    val connFactory = new DefaultConnectionFactory()
    //    connFactory.setConnectionConfig(connConfig)
    //
    //    val poolConfig = new PoolConfig()
    //    poolConfig.setMinPoolSize(configuration.get[Int]("ldap.min-connection-pool-size"))
    //    poolConfig.setMaxPoolSize(configuration.get[Int]("ldap.max-connection-poll-size"))
    //    poolConfig.setValidateOnCheckOut(true)
    //    poolConfig.setValidateOnCheckIn(true)
    //    poolConfig.setValidatePeriodically(false)
    //
    //    val searchValidator = new SearchValidator()
    //    val pruneStrategy = new IdlePruneStrategy()
    //
    //    val connPool = new BlockingConnectionPool()
    //    connPool.setPoolConfig(poolConfig)
    //    connPool.setBlockWaitTime(Duration.ofMillis(configuration.get[Long]("ldap.connection-block-wait-time")))
    //    connPool.setValidator(searchValidator)
    //    connPool.setPruneStrategy(pruneStrategy)
    //    connPool.setConnectionFactory(connFactory)
    //    connPool.initialize()
    //
    //    val pooledConnectionFactory = new PooledConnectionFactory()
    //    pooledConnectionFactory.setConnectionPool(connPool)
    //
    //    val handler = new PooledBindAuthenticationHandler()
    //    handler.setConnectionFactory(pooledConnectionFactory)
    //
    //    val dnResolver = new SearchDnResolver(pooledConnectionFactory)
    //    dnResolver.setBaseDn(configuration.get[String]("ldap.users-dn"))
    //    dnResolver.setUserFilter("(uid={user})")
    //
    //    val authenticator = new Authenticator()
    //    authenticator.setDnResolver(dnResolver)
    //    authenticator.setAuthenticationHandler(handler)
    //
    //    val ldapProfileService = new LdapProfileService(connFactory, authenticator, configuration.get[String]("ldap.users-dn"))
  }

  @Provides
  def provideDirectFormClient: DirectFormClient = new DirectFormClient(new SimpleTestUsernamePasswordAuthenticator())

  @Provides
  def provideConfig(directFormClient: DirectFormClient): Config = {
    val clients = new Clients(directFormClient)
    val config = new Config(clients)
    config.setHttpActionAdapter(new SecurityHttpActionAdapter())
    config.addAuthorizer("login", new LoginAuthorizer())
    config
  }
}
