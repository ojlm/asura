package asura.common.util

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.{BodyHandler, BodyHandlers, BodySubscribers, ResponseInfo}
import java.net.http.{HttpClient, HttpHeaders, HttpRequest, HttpResponse}
import java.nio.charset.{Charset, StandardCharsets}
import java.time.Duration

import jdk.internal.net.http.common.Log
import sun.net.www.HeaderParser

import scala.concurrent.Future
import scala.jdk.FutureConverters._

object HttpUtils {

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(60))
    .build()

  // import jdk.internal.net.http.common.Utils.charsetFrom
  private def charsetFrom(headers: HttpHeaders): Charset = {
    var contentType = headers.firstValue("Content-type").orElse("text/html; charset=utf-8")
    val i = contentType.indexOf(";")
    if (i >= 0) contentType = contentType.substring(i + 1)
    try {
      val parser = new HeaderParser(contentType)
      val value = parser.findValue("charset")
      if (value == null) {
        StandardCharsets.UTF_8
      } else {
        Charset.forName(value)
      }
    } catch {
      case x: Throwable =>
        Log.logTrace("Can't find charset in \"{0}\" ({1})", contentType, x)
        StandardCharsets.UTF_8
    }
  }

  def getTypeBodyHandler[T <: AnyRef](clazz: Class[T]): BodyHandler[T] = {
    (responseInfo: ResponseInfo) => {
      val charset = charsetFrom(responseInfo.headers)
      BodySubscribers.mapping(
        BodySubscribers.ofByteArray(),
        (bytes: Array[Byte]) => JsonUtils.parse(new String(bytes, charset), clazz)
      )
    }
  }

  def sendAsync(request: HttpRequest): Future[HttpResponse[String]] = {
    httpClient.sendAsync(request, BodyHandlers.ofString()).asScala
  }

  def sendAsync[T <: AnyRef](request: HttpRequest, clazz: Class[T]): Future[HttpResponse[T]] = {
    httpClient.sendAsync(request, getTypeBodyHandler(clazz)).asScala
  }

  def sendAsyncGetBody[T <: AnyRef](request: HttpRequest, clazz: Class[T]): Future[T] = {
    httpClient.sendAsync(request, getTypeBodyHandler(clazz)).thenApply(res => res.body()).asScala
  }

  def getAsync[T <: AnyRef](url: String, clazz: Class[T]): Future[T] = {
    getAsync(url, null, clazz)
  }

  def postJson[T <: AnyRef](url: String, body: Object, clazz: Class[T]): Future[T] = {
    postJson(url, body, null, clazz)
  }

  def getAsync[T <: AnyRef](url: String, headers: Map[String, String], clazz: Class[T]): Future[T] = {
    val builder = HttpRequest.newBuilder(URI.create(url)).GET()
    if (null != headers) headers.foreach(header => builder.header(header._1, header._2))
    sendAsyncGetBody(builder.build(), clazz)
  }

  def postJson[T <: AnyRef](url: String, body: Object, headers: Map[String, String], clazz: Class[T]): Future[T] = {
    val bodyStr = if (body.isInstanceOf[String]) body.asInstanceOf[String] else JsonUtils.stringify(body)
    val builder = HttpRequest.newBuilder(URI.create(url))
      .POST(BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))
    builder.header("Content-Type", "application/json")
    if (null != headers) headers.foreach(header => builder.header(header._1, header._2))
    sendAsyncGetBody(builder.build(), clazz)
  }

}
