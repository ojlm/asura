package asura.dubbo

object GenericServiceInvoker {

  def main(args: Array[String]): Unit = {

    val genericRequest = GenericRequest(
      dubboGroup = "",
      interface = "asura.dubbo.service.EchoService",
      method = "echoString",
      parameterTypes = Array("java.lang.String", "int"),
      args = Array("world", new Integer(27)),
      address = "127.0.0.1",
      port = 20880,
      version = ""
    )
    val service = genericRequest.toReferenceConfig().get()
    val result = service.$invoke(genericRequest.method, genericRequest.parameterTypes, genericRequest.args)
    println(result, result.getClass.getCanonicalName)
  }
}
