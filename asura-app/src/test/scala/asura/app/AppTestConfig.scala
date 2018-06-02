package asura.app

import akka.stream.ActorMaterializer
import asura.AppConfig
import asura.common.AkkaTestKitBaseSpec
import asura.core.CoreConfig

trait AppTestConfig extends AkkaTestKitBaseSpec {
  CoreConfig.init(CoreConfig(
    system = system,
    dispatcher = dispatcher,
    materializer = ActorMaterializer(),
    redisServers = AppConfig.redisServer,
    esUrl = AppConfig.esUrl,
    proxyHost = AppConfig.linkerProxyHost,
    httpProxyPort = AppConfig.linkerHttpProxyPort,
    httpsProxyPort = AppConfig.linkerHttpsProxyPort,
    proxyIdentifier = AppConfig.linkerHeaderIdentifier,
    reportBaseUrl = AppConfig.reportBaseUrl
  ))
}
