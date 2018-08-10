package asura.app.api.auth

import java.time.{Duration, LocalDate, ZoneId}
import java.util.Date

import org.ldaptive._
import org.ldaptive.auth.{Authenticator, PooledBindAuthenticationHandler, PooledSearchDnResolver}
import org.ldaptive.pool._
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.profile.CommonProfile
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.ldap.profile.service.LdapProfileService
import play.api.Configuration

object LdapAuthenticator {

  def apply(configuration: Configuration): LdapProfileService = {
    val connConfig = new ConnectionConfig()
    connConfig.setConnectTimeout(Duration.ofMillis(configuration.get[Long]("asura.ldap.connection-timeout")))
    connConfig.setResponseTimeout(Duration.ofMillis(configuration.get[Long]("asura.ldap.response-timeout")))
    connConfig.setLdapUrl(configuration.get[String]("asura.ldap.url"))
    connConfig.setConnectionInitializer(new BindConnectionInitializer(configuration.get[String]("asura.ldap.bind-dn"), new Credential(configuration.get[String]("asura.ldap.password"))))

    val connFactory = new DefaultConnectionFactory()
    connFactory.setConnectionConfig(connConfig)

    val poolConfig = new PoolConfig()
    poolConfig.setMinPoolSize(configuration.get[Int]("asura.ldap.min-connection-pool-size"))
    poolConfig.setMaxPoolSize(configuration.get[Int]("asura.ldap.max-connection-poll-size"))
    poolConfig.setValidateOnCheckOut(true)
    poolConfig.setValidateOnCheckIn(true)
    poolConfig.setValidatePeriodically(false)

    val searchValidator = new SearchValidator()
    val pruneStrategy = new IdlePruneStrategy()

    val connPool = new BlockingConnectionPool()
    connPool.setPoolConfig(poolConfig)
    connPool.setBlockWaitTime(Duration.ofMillis(configuration.get[Long]("asura.ldap.connection-block-wait-time")))
    connPool.setValidator(searchValidator)
    connPool.setPruneStrategy(pruneStrategy)
    connPool.setConnectionFactory(connFactory)
    connPool.initialize()

    val pooledConnectionFactory = new PooledConnectionFactory()
    pooledConnectionFactory.setConnectionPool(connPool)

    val handler = new PooledBindAuthenticationHandler()
    handler.setConnectionFactory(pooledConnectionFactory)

    val dnResolver = new PooledSearchDnResolver(pooledConnectionFactory)
    dnResolver.setBaseDn(configuration.get[String]("asura.ldap.searchbase"))
    dnResolver.setSubtreeSearch(true)
    dnResolver.setUserFilter("(uid={user})")
    val authenticator = new Authenticator()
    authenticator.setDnResolver(dnResolver)
    authenticator.setAuthenticationHandler(handler)

    new CustomLdapProfileService(configuration, connFactory, authenticator, configuration.get[String]("asura.ldap.searchbase"))
  }

  class CustomLdapProfileService(
                                  configuration: Configuration,
                                  connectionFactory: ConnectionFactory,
                                  authenticator: Authenticator,
                                  usersDn: String)
    extends LdapProfileService(connectionFactory, authenticator, usersDn) {

    this.setIdAttribute("uid")
    this.setAttributes("mail")

    override def validate(credentials: UsernamePasswordCredentials, context: WebContext): Unit = {
      super.validate(credentials, context)
      val jwtGenerator = new JwtGenerator[CommonProfile](new SecretSignatureConfiguration(configuration.get[String]("asura.jwt.secret")))
      val tomorrow = LocalDate.now().plusDays(1).atStartOfDay()
      jwtGenerator.setExpirationTime(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()))
      val profile = credentials.getUserProfile
      val token = jwtGenerator.generate(profile)
      profile.addAttribute("token", token)
    }
  }

}
