package asura.core.http

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.headers.{Cookie, RawHeader}
import akka.http.scaladsl.model.{ErrorInfo, HttpHeader}
import asura.common.exceptions.InvalidStatusException
import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.cs.CaseContext
import asura.core.es.model.{Case, Environment, RestApi}
import com.typesafe.scalalogging.Logger

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object HeaderUtils {

  val logger = Logger("HeaderUtils")

  def toHeaders(cs: Case, context: CaseContext, api: RestApi, env: Environment = null): immutable.Seq[HttpHeader] = {
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
      if (null == api || StringUtils.isEmpty(api.service) || StringUtils.isEmpty(cs.namespace)) {
        throw InvalidStatusException(
          s"invalid data when use proxy, ns: ${cs.namespace}, service: ${if (null == api) "null" else api.service}")
      } else {
        val dst = StringBuilder.newBuilder
        dst.append("/").append(cs.namespace).append("/").append(api.service)
        headers += RawHeader(CoreConfig.proxyIdentifier, dst.toString)
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
