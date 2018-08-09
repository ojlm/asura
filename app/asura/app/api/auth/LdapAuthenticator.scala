package asura.app.api.auth

import java.time.Duration

import org.ldaptive.auth.{Authenticator, PooledBindAuthenticationHandler, PooledSearchDnResolver}
import org.ldaptive.pool._
import org.ldaptive.{BindConnectionInitializer, ConnectionConfig, Credential, DefaultConnectionFactory}
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
    dnResolver.setUserFilter("(uid={user})")

    val authenticator = new Authenticator()
    authenticator.setDnResolver(dnResolver)
    authenticator.setAuthenticationHandler(handler)

    new LdapProfileService(connFactory, authenticator, configuration.get[String]("asura.ldap.searchbase"))
  }
}
