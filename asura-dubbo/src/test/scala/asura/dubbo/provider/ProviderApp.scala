package asura.dubbo.provider

import asura.dubbo.provider.impl.EchoServiceImpl
import asura.dubbo.service.EchoService
import asura.dubbo.{EmbeddedZooKeeper, TestConfig}
import com.alibaba.dubbo.config.{ApplicationConfig, RegistryConfig, ServiceConfig}

object ProviderApp extends TestConfig {

  def main(args: Array[String]): Unit = {
    new EmbeddedZooKeeper(2181, false).start()
    val echoService = new ServiceConfig[EchoService]
    echoService.setApplication(new ApplicationConfig("asura-dubbo"))
    echoService.setRegistry(new RegistryConfig(zkAddr))
    echoService.setInterface(classOf[EchoService])
    echoService.setRef(new EchoServiceImpl())
    echoService.export()
    System.in.read()
  }
}
