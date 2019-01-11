package asura.dubbo

import com.alibaba.dubbo.config.ApplicationConfig

case class DubboConfig(
                        appName: String = "asura-dubbo"
                      ) {

}

object DubboConfig {

  val DEFAULT_PROTOCOL = "dubbo://"
  val DEFAULT_PORT = "20880"
  val DEFAULT_ROOT_DUBBO_PATH = "/dubbo"
  var appName = "asura-dubbo"
  var appConfig = new ApplicationConfig(appName)
}
