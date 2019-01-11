package asura.dubbo

import asura.dubbo.cache.ReferenceCache

object GenericServiceInvoker {

  def main(args: Array[String]): Unit = {

    val genericRequest = GenericRequest(
      group = "indigo",
      project = "docs",
      dubboGroup = "",
      interface = "asura.dubbo.service.EchoService",
      method = "echoString",
      parameterTypes = Array("java.lang.String"),
      args = Array("world"),
      address = "127.0.0.1",
      port = 20880,
      version = ""
    )
    val (service, refConfig) = ReferenceCache.getServiceAndConfig(genericRequest)
    val result = service.$invoke(genericRequest.method, genericRequest.parameterTypes, genericRequest.args)
    ReferenceCache.destroyReference(refConfig)
    println(result, result.getClass.getCanonicalName)
  }
}
