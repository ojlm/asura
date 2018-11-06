package asura.core.http

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

import akka.http.scaladsl.model.Uri
import asura.common.exceptions.InvalidStatusException
import asura.core.cs.CaseContext
import asura.core.es.model.Case
import asura.core.util.StringTemplate

object UriUtils {

  val UTF8 = StandardCharsets.UTF_8.name()

  def toUri(cs: Case, context: CaseContext): Uri = {
    Uri.from(
      scheme = cs.request.protocol,
      host = cs.request.host,
      port = cs.request.port,
      path = renderPath(URLDecoder.decode(cs.request.urlPath, StandardCharsets.UTF_8.name()), cs, context),
      queryString = buildQueryString(cs, context)
    )
  }

  def mapToQueryString(map: Map[String, Any], context: CaseContext = null): String = {
    val sb = StringBuilder.newBuilder
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

  @throws[InvalidStatusException]("if path template variable not in cs")
  def renderPath(tpl: String, cs: Case, context: CaseContext): String = {
    if (null != cs.request) {
      val params = cs.request.path
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

  def buildQueryString(cs: Case, context: CaseContext): Option[String] = {
    if (null != cs.request) {
      val params = cs.request.query
      if (null != params && params.nonEmpty) {
        val sb = StringBuilder.newBuilder
        for (param <- params if param.enabled) {
          sb.append(param.key)
            .append("=")
            .append(context.renderSingleMacroAsString(param.value))
            .append("&")
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
