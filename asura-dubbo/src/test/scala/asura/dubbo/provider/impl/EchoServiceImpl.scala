package asura.dubbo.provider.impl

import asura.dubbo.service.EchoService

class EchoServiceImpl extends EchoService {

  override def echoString(text: String): String = s"Hello ${text}"
}
