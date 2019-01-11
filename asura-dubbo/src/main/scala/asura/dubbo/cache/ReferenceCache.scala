package asura.dubbo.cache

import asura.dubbo.GenericRequest
import com.alibaba.dubbo.config.ReferenceConfig
import com.alibaba.dubbo.config.utils.ReferenceConfigCache
import com.alibaba.dubbo.rpc.service.GenericService

object ReferenceCache {

  def getServiceAndConfig(request: GenericRequest): (GenericService, ReferenceConfig[GenericService]) = {
    val cache = ReferenceConfigCache.getCache()
    val referenceConfig = request.toReferenceConfig()
    var service = cache.get(referenceConfig)
    if (null == service) {
      destroyReference(referenceConfig)
      service = cache.get(referenceConfig)
    }
    (service, referenceConfig)
  }

  def destroyReference(reference: ReferenceConfig[GenericService]): Unit = {
    val cache = ReferenceConfigCache.getCache()
    cache.destroy(reference)
  }
}
