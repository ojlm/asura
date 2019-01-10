package asura.dubbo

import com.alibaba.dubbo.config.ApplicationConfig

case class DubboConfig(
                        appName: String = "asura-dubbo"
                      ) {

}

object DubboConfig {

  val DEFAULT_ROOT_DUBBO_PATH = "/dubbo"
  var appName = "asura-dubbo"
  var appConfig = new ApplicationConfig(appName)
}
