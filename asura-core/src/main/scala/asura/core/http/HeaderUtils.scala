package asura.core.http

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.headers.{Cookie, RawHeader}
import akka.http.scaladsl.model.{ErrorInfo, HttpHeader}
import asura.common.util.StringUtils
import asura.core.cs.CaseContext
import asura.core.es.model.{Case, Environment}
import asura.core.{CoreConfig, ErrorMessages}
import com.typesafe.scalalogging.Logger

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object HeaderUtils {

  val logger = Logger("HeaderUtils")

  def toHeaders(cs: Case, context: CaseContext, env: Environment = null): immutable.Seq[HttpHeader] = {
    val headers = ArrayBuffer[HttpHeader]()
    val request = cs.request
    if (null != request) {
      val headerSeq = request.header
      if (null != headerSeq) {
        for (h <- headerSeq if h.enabled) {
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
        for (c <- cookieSeq if c.enabled) {
          headers += Cookie(c.key, context.renderSingleMacroAsString(c.value))
        }
      }
    }
    if (Option(cs.useProxy).isDefined && cs.useProxy) {
      val ns = if (null != env) {
        env.namespace
      } else {
        cs.namespace
      }
      if (StringUtils.isNotEmpty(ns)) {
        val dst = StringBuilder.newBuilder
        dst.append("/").append(cs.namespace).append("/").append(cs.group).append("/").append(cs.project)
        headers += RawHeader(CoreConfig.proxyIdentifier, dst.toString)
      } else {
        ErrorMessages.error_EmptyNamespace.toFutureFail
      }
    }
    headers.toList
  }

  def isApplicationJson(header: HttpHeader): Boolean = {
    if (header.lowercaseName().equals("content-type")) {
      header.value().contains(HttpContentTypes.JSON)
    } else {
      false
    }
  }
}
