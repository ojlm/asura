package asura.core.http

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.headers.{Cookie, RawHeader}
import akka.http.scaladsl.model.{ErrorInfo, HttpHeader}
import asura.common.util.StringUtils
import asura.core.runtime.RuntimeContext
import asura.core.es.model.{HttpCaseRequest, Environment}
import asura.core.{CoreConfig, ErrorMessages}
import com.typesafe.scalalogging.Logger

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object HeaderUtils {

  val logger = Logger("HeaderUtils")

  def toHeaders(cs: HttpCaseRequest, context: RuntimeContext): immutable.Seq[HttpHeader] = {
    val headers = ArrayBuffer[HttpHeader]()
    val request = cs.request
    val env = if (null != context.options) context.options.getUsedEnv() else null
    if (null != request) {
      val headerSeq = request.header
      if (null != headerSeq) {
        for (h <- headerSeq if (h.enabled && StringUtils.isNotEmpty(h.key))) {
          HttpHeader.parse(h.key, context.renderSingleMacroAsString(h.value)) match {
            case Ok(header: HttpHeader, errors: List[ErrorInfo]) =>
              if (errors.nonEmpty) logger.warn(errors.mkString(","))
              headers += header
            case Error(error: ErrorInfo) =>
              logger.warn(error.detail)
          }
        }
      }
      val cookieSeq = request.cookie
      if (null != cookieSeq) {
        for (c <- cookieSeq if (c.enabled && StringUtils.isNotEmpty(c.key))) {
          headers += Cookie(c.key, context.renderSingleMacroAsString(c.value))
        }
      }
    }
    if (null != env && null != env.headers && env.headers.nonEmpty) {
      for (h <- env.headers if (h.enabled && StringUtils.isNotEmpty(h.key))) {
        HttpHeader.parse(h.key, context.renderSingleMacroAsString(h.value)) match {
          case Ok(header: HttpHeader, errors: List[ErrorInfo]) =>
            if (errors.nonEmpty) logger.warn(errors.mkString(","))
            headers += header
          case Error(error: ErrorInfo) =>
            logger.warn(error.detail)
        }
      }
    }
    if (null != env && env.enableProxy) {
      val headerIdentifier = validateProxyVariables(env)
      val dst = StringBuilder.newBuilder
      dst.append("/").append(cs.group).append("/").append(cs.project).append("/").append(env.namespace)
      headers += RawHeader(headerIdentifier, dst.toString)
    }
    headers.toList
  }

  def validateProxyVariables(env: Environment): String = {
    if (!CoreConfig.linkerdConfig.enabled) {
      throw ErrorMessages.error_ProxyDisabled.toException
    }
    if (StringUtils.isEmpty(env.namespace)) {
      throw ErrorMessages.error_EmptyNamespace.toException
    }
    if (StringUtils.isEmpty(env.server)) {
      throw ErrorMessages.error_EmptyProxyServer.toException
    }
    val proxyServerOpt = CoreConfig.linkerdConfig.servers.find(_.tag.equals(env.server))
    if (proxyServerOpt.isEmpty && StringUtils.isEmpty(proxyServerOpt.get.headerIdentifier)) {
      throw ErrorMessages.error_InvalidProxyConfig.toException
    } else {
      proxyServerOpt.get.headerIdentifier
    }
  }

  def isApplicationJson(header: HttpHeader): Boolean = {
    if (header.lowercaseName().equals("content-type")) {
      header.value().contains(HttpContentTypes.JSON)
    } else {
      false
    }
  }
}
