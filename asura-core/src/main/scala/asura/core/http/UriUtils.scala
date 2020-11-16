package asura.core.http

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

import akka.http.scaladsl.model.Uri
import asura.common.util.StringUtils
import asura.core.es.model.HttpStepRequest
import asura.core.protocols.Protocols
import asura.core.runtime.RuntimeContext
import asura.core.util.StringTemplate

object UriUtils {

  val UTF8 = StandardCharsets.UTF_8.name()

  def toUri(httpRequest: HttpStepRequest, context: RuntimeContext): Uri = {
    Uri.from(
      scheme = StringUtils.notEmptyElse(httpRequest.request.protocol, Protocols.HTTP),
      host = context.renderSingleMacroAsString(URLDecoder.decode(httpRequest.request.host, UTF8)),
      port = if (httpRequest.request.port < 0 || httpRequest.request.port > 65535) 80 else httpRequest.request.port,
      path = renderPath(URLDecoder.decode(httpRequest.request.urlPath, UTF8), httpRequest, context),
      queryString = buildQueryString(httpRequest, context)
    )
  }

  def mapToQueryString(map: Map[String, Any], context: RuntimeContext = null): String = {
    val sb = new StringBuilder()
    for ((k, v) <- map) {
      v match {
        case v: String =>
          val renderedValue = if (null != context) context.renderSingleMacroAsString(v) else v
          sb.append(k).append("=").append(URLEncoder.encode(renderedValue, UTF8)).append("&")
        case v: List[_] =>
          v.foreach(i => {
            val value = i.toString
            val renderedValue = if (null != context) context.renderSingleMacroAsString(value) else value
            sb.append(k).append("=").append(URLEncoder.encode(renderedValue, UTF8)).append("&")
          })
      }
    }
    if (sb.nonEmpty) {
      sb.deleteCharAt(sb.length - 1)
    }
    sb.toString
  }

  def renderPath(tpl: String, httpRequest: HttpStepRequest, context: RuntimeContext): String = {
    if (null != httpRequest.request) {
      val params = httpRequest.request.path
      if (null != params && params.nonEmpty) {
        val ctx = params.map(param => param.key -> context.renderSingleMacroAsString(param.value)).toMap
        StringTemplate.uriPathParse(tpl, ctx)
      } else {
        tpl
      }
    } else {
      tpl
    }
  }

  def buildQueryString(httpRequest: HttpStepRequest, context: RuntimeContext): Option[String] = {
    if (null != httpRequest.request) {
      val params = httpRequest.request.query
      if (null != params && params.nonEmpty) {
        val sb = new StringBuilder()
        for (param <- params if param.enabled) {
          val key = if (StringUtils.isNotEmpty(param.key)) {
            URLEncoder.encode(param.key, UTF8)
          } else {
            StringUtils.EMPTY
          }
          val value = if (StringUtils.isNotEmpty(param.value)) {
            URLEncoder.encode(context.renderSingleMacroAsString(param.value), UTF8)
          } else {
            StringUtils.EMPTY
          }
          sb.append(key).append("=").append(value).append("&")
        }
        if (sb.nonEmpty) {
          sb.deleteCharAt(sb.length - 1)
        }
        Some(sb.toString)
      } else {
        None
      }
    } else {
      None
    }
  }
}
