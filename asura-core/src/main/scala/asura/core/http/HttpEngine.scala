package asura.core.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.{ErrorInfo, HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import asura.common.model.{ApiMsg, BoolErrorRes, BoolErrorTypeRes}
import asura.common.util.LogUtils
import asura.core.CoreConfig
import asura.core.CoreConfig._
import asura.core.protocols.Protocols
import asura.core.util.JacksonSupport
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object HttpEngine {

  val logger = Logger("HttpEngine")
  val http = Http()

  /**
    * This method will use linkerd proxy and set a `Host` header of original `host` default.
    * If the schema is `https`, it will replaced by `http` to make the linkerd handle https traffic.
    **/
  def singleRequestWithProxy(
                              request: HttpRequest,
                              proxyHost: String = CoreConfig.proxyHost,
                              httpProxyPort: Int = CoreConfig.httpProxyPort,
                              httpsProxyPort: Int = CoreConfig.httpsProxyPort,
                            ): Future[HttpResponse] = {
    val originUri = request.uri
    val host = originUri.authority.host.address()
    val proxyRequest = if (Protocols.HTTPS.equals(request.uri.scheme)) {
      val proxyUri = originUri.withScheme(Protocols.HTTP).withAuthority(proxyHost, httpsProxyPort)
      request.withUri(proxyUri)
    } else {
      val proxyUri = originUri.withAuthority(proxyHost, httpProxyPort)
      request.withUri(proxyUri)
    }
    val hostHeader: HttpHeader = HttpHeader.parse("Host", host) match {
      case Ok(header: HttpHeader, errors: List[ErrorInfo]) =>
        if (errors.nonEmpty) logger.warn(errors.mkString(","))
        header
      case Error(error: ErrorInfo) =>
        logger.warn(error.detail)
        null
    }
    val proxyHeaders = if (null != hostHeader) proxyRequest.headers :+ hostHeader else proxyRequest.headers
    http.singleRequest(proxyRequest.withHeaders(proxyHeaders))
  }

  def singleRequest(request: HttpRequest): Future[HttpResponse] = http.singleRequest(request)

  def singleRequestStr(request: HttpRequest): Future[String] = {
    http.singleRequest(request).flatMap(res =>
      Unmarshal(res.entity).to[String]
    )
  }

  def singleRequest[T >: Null <: AnyRef](request: HttpRequest, clazz: Class[T]): Future[T] = {
    http.singleRequest(request).flatMap(res =>
      Unmarshal(res.entity).to[String].map(str =>
        JacksonSupport.parse(str, clazz)
      )
    )
  }

  // will block current thread
  def getStringBlock(request: HttpRequest): BoolErrorRes = {
    try {
      val futureResponse = http.singleRequest(request)
      val response = Await.result(futureResponse, Duration.Inf)
      val futureStr = Unmarshal(response.entity).to[String]
      (true, Await.result(futureStr, Duration.Inf))
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        (false, t.getMessage)
    }
  }

  // will block current thread
  def getResponseBlock[T >: Null <: AnyRef](request: HttpRequest, clazz: Class[T]): BoolErrorTypeRes[T] = {
    try {
      val futureResponse = http.singleRequest(request)
      val response = Await.result(futureResponse, Duration.Inf)
      val futureStr = Unmarshal(response.entity).to[String]
      val str = Await.result(futureStr, Duration.Inf)
      (true, ApiMsg.SUCCESS, JacksonSupport.parse(str, clazz))
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        (false, t.getMessage, null)
    }
  }
}
