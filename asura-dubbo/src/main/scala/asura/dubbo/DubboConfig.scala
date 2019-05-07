package asura.dubbo

import java.util.concurrent.Executors

import com.alibaba.dubbo.config.ApplicationConfig

import scala.concurrent.ExecutionContext

case class DubboConfig(
                        appName: String = "asura-dubbo"
                      ) {

}

object DubboConfig {

  val DEFAULT_PROTOCOL = "dubbo://"
  val DEFAULT_PORT = 20880
  val DEFAULT_ROOT_DUBBO_PATH = "/dubbo"
  val DEFAULT_PROMPT = "dubbo>"
  val DEFAULT_ZK_CLIENT_CACHE_SIZE = 10
  val DEFAULT_DUBBO_REF_CACHE_SIZE = 20
  val DEFAULT_TIMEOUT = 10000
  var appName = "asura-dubbo"
  var appConfig = new ApplicationConfig(appName)

  val DUBBO_EC = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
}
