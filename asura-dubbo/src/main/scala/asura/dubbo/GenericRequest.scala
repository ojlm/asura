package asura.dubbo

import asura.common.util.StringUtils
import com.alibaba.dubbo.config.ReferenceConfig
import com.alibaba.dubbo.rpc.service.GenericService

case class GenericRequest(
                           group: String,
                           project: String,
                           dubboGroup: String,
                           interface: String,
                           method: String,
                           parameterTypes: Array[String],
                           args: Array[Object],
                           address: String,
                           version: String
                         ) {

  def toReferenceConfig(): ReferenceConfig[GenericService] = {
    val referenceConfig = new ReferenceConfig[GenericService]()
    if (StringUtils.isNotEmpty(dubboGroup)) {
      referenceConfig.setGroup(dubboGroup)
    }
    referenceConfig.setApplication(DubboConfig.appConfig)
    referenceConfig.setUrl(address)
    referenceConfig.setInterface(interface)
    referenceConfig.setGeneric(true)
    if (StringUtils.isNotEmpty(version)) {
      referenceConfig.setVersion(version)
    }
    referenceConfig
  }
}
