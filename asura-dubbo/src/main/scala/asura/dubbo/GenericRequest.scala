package asura.dubbo

import asura.common.util.{JavaJsonUtils, JsonUtils, StringUtils}
import com.alibaba.dubbo.config.ReferenceConfig
import com.alibaba.dubbo.rpc.service.GenericService

import scala.collection.mutable.StringBuilder

case class GenericRequest(
                           dubboGroup: String,
                           interface: String,
                           method: String,
                           parameterTypes: Array[String],
                           args: Array[Object],
                           address: String,
                           port: Int,
                           version: String
                         ) {

  def getParameterTypes(): Array[String] = {
    if (null != parameterTypes) parameterTypes else Array.empty[String]
  }

  def getArgs(): Array[Object] = {
    if (null != args) {
      args.map(typeConvert)
    } else {
      Array.empty[Object]
    }
  }

  def toReferenceConfig(): ReferenceConfig[GenericService] = {
    val referenceConfig = new ReferenceConfig[GenericService]()
    if (StringUtils.isNotEmpty(dubboGroup)) {
      referenceConfig.setGroup(dubboGroup)
    }
    referenceConfig.setApplication(DubboConfig.appConfig)
    referenceConfig.setUrl(toDubboUrl())
    referenceConfig.setInterface(interface)
    referenceConfig.setGeneric(true)
    referenceConfig.setTimeout(DubboConfig.DEFAULT_TIMEOUT)
    if (StringUtils.isNotEmpty(version)) {
      referenceConfig.setVersion(version)
    }
    referenceConfig
  }

  /**
   * reference to [[com.alibaba.dubbo.config.utils.ReferenceConfigCache.DEFAULT_KEY_GENERATOR]]
   * key example: `group1/com.alibaba.foo.FooService:1.0.0@127.0.0.1:20880`.
   */
  def generateCacheKey(): String = {
    val sb = new StringBuilder()
    if (StringUtils.isNotEmpty(dubboGroup)) {
      sb.append(dubboGroup).append("/")
    }
    if (StringUtils.isNotEmpty(interface)) {
      sb.append(interface)
    }
    if (StringUtils.isNotEmpty(version)) {
      sb.append(":").append(version)
    }
    if (StringUtils.isNotEmpty(address)) {
      sb.append("@").append(address).append(":").append(port)
    }
    sb.toString()
  }

  def toDubboUrl() = {
    val portStr = if (port > 0) port.toString else DubboConfig.DEFAULT_PORT.toString
    s"${DubboConfig.DEFAULT_PROTOCOL}${address}:${portStr}"
  }

  def validate(): Boolean = {
    if (
      StringUtils.isEmpty(interface) ||
        StringUtils.isEmpty(method) ||
        StringUtils.isEmpty(address) ||
        !(null != parameterTypes && null != args && parameterTypes.length == args.length)
    ) {
      false
    } else {
      true
    }
  }

  // convert to java types
  def typeConvert(arg: Object): Object = {
    arg match {
      case _: Map[_, _] =>
        JavaJsonUtils.parse(JsonUtils.stringify(arg), classOf[Object])
      case _: Seq[_] =>
        JavaJsonUtils.parse(JsonUtils.stringify(arg), classOf[Object])
      case _ => arg
    }
  }
}
