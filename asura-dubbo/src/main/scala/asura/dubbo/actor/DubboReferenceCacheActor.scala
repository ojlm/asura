package asura.dubbo.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.cache.LRUCache
import asura.dubbo.{DubboConfig, GenericRequest}
import com.alibaba.dubbo.config.ReferenceConfig
import com.alibaba.dubbo.config.utils.ReferenceConfigCache
import com.alibaba.dubbo.rpc.service.GenericService

import scala.concurrent.{ExecutionContext, Future}

class DubboReferenceCacheActor extends BaseActor {

  private val lruCache = LRUCache[String, ReferenceConfig[GenericService]](DubboConfig.DEFAULT_DUBBO_REF_CACHE_SIZE, (_, ref) => {
    ref.destroy()
  })
  implicit val actorEC: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case request: GenericRequest =>
      test(request) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def test(request: GenericRequest): Future[Object] = {
    Future {
      val refConfig = request.toReferenceConfig()
      val cacheKey = ReferenceConfigCache.DEFAULT_KEY_GENERATOR.generateKey(refConfig)
      val value = lruCache.get(cacheKey)
      val service = if (null == value) {
        val newService = refConfig.get()
        if (null != newService) lruCache.put(cacheKey, refConfig)
        newService
      } else {
        value.get()
      }
      if (null != service) {
        // https://github.com/apache/incubator-dubbo/issues/3163
        val args = request.getArgs()
        service.$invoke(request.method, request.getParameterTypes(), args)
      } else {
        new RuntimeException("Null dubbo generic service from reference config")
      }
    }(DubboConfig.DUBBO_EC)
  }

  override def postStop(): Unit = {
    log.debug(s"Destroy ReferenceConfig size: ${lruCache.size()}")
    lruCache.forEach((_, ref) => {
      ref.destroy()
    })
  }
}

object DubboReferenceCacheActor {
  def props() = Props(new DubboReferenceCacheActor())
}
