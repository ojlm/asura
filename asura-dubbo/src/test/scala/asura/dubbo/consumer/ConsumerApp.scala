package asura.dubbo.consumer

import asura.dubbo.TestConfig
import asura.dubbo.service.EchoService
import com.alibaba.dubbo.config.{ApplicationConfig, ReferenceConfig, RegistryConfig}

object ConsumerApp extends TestConfig {

  def main(args: Array[String]): Unit = {
    val reference = new ReferenceConfig[EchoService]
    reference.setApplication(new ApplicationConfig(appName))
    reference.setRegistry(new RegistryConfig(zkAddr))
    reference.setInterface(classOf[EchoService])
    val echoService = reference.get
    val result = echoService.echoString("world", 27)
    println(result)
  }
}
