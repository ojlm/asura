package asura.core.cs.scenario

import akka.stream.ActorMaterializer
import asura.common.AkkaTestKitBaseSpec
import asura.core.CoreConfig
import asura.core.es.EsClientConfig

class ScenarioRunnerSpec extends AkkaTestKitBaseSpec with EsClientConfig {

  CoreConfig.dispatcher = dispatcher
  CoreConfig.system = system
  CoreConfig.materializer = ActorMaterializer()
  CoreConfig.proxyIdentifier = "asura-header"
  CoreConfig.httpProxyPort = 4140
  CoreConfig.httpsProxyPort = 4143
  CoreConfig.proxyHost = "localhost"

}
