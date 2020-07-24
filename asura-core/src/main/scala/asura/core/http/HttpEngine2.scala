package asura.core.http

import java.net.http.HttpResponse.{BodyHandler, BodyHandlers, BodySubscribers}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

import asura.common.util.JsonUtils
import jdk.internal.net.http.common.Utils.charsetFrom

import scala.concurrent.Future
import scala.jdk.FutureConverters._

object HttpEngine2 {

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .build()

  def sendAsync(request: HttpRequest): Future[HttpResponse[String]] = {
    httpClient.sendAsync(request, BodyHandlers.ofString()).asScala
  }

  def sendAsync[T <: AnyRef](request: HttpRequest, clazz: Class[T]): Future[HttpResponse[T]] = {
    val handler: BodyHandler[T] = (responseInfo) => {
      val charset = charsetFrom(responseInfo.headers)
      BodySubscribers.mapping(
        BodySubscribers.ofByteArray(),
        (bytes: Array[Byte]) => JsonUtils.parse(new String(bytes, charset), clazz)
      )
    }
    httpClient.sendAsync(request, handler).asScala
  }
}
