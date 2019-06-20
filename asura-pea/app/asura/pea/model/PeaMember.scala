package asura.pea.model

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets

import asura.common.util.StringUtils

import scala.collection.mutable

case class PeaMember(
                      address: String,
                      port: Int,
                      hostname: String,
                    )

object PeaMember {

  def apply(uriStr: String): PeaMember = {
    try {
      val uri = URI.create(URLDecoder.decode(uriStr, StandardCharsets.UTF_8.name()))
      val queryMap = mutable.Map[String, String]()
      uri.getQuery.split("&").foreach(paramStr => {
        val param = paramStr.split("=")
        if (param.length == 2) {
          queryMap += (param(0) -> param(1))
        }
      })
      PeaMember(uri.getHost, uri.getPort, queryMap.getOrElse("hostname", StringUtils.EMPTY))
    } catch {
      case _: Throwable => null
    }
  }
}
