package asura.core.http

import java.net.URLEncoder

import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, RequestEntity}
import akka.util.ByteString
import asura.common.util.{LogUtils, StringUtils}
import asura.core.cs.CaseContext
import asura.core.es.model.{Case, KeyValueObject}
import asura.core.http.UriUtils.UTF8
import asura.core.util.JacksonSupport
import com.fasterxml.jackson.core.`type`.TypeReference
import com.typesafe.scalalogging.Logger

object EntityUtils {

  val logger = Logger("EntityUtils")

  def toEntity(cs: Case, context: CaseContext): RequestEntity = {
    val request = cs.request
    var contentType: ContentType = ContentTypes.`text/plain(UTF-8)`
    var byteString: ByteString = ByteString.empty
    if (StringUtils.isNotEmpty(request.contentType) && null != request.body && request.body.nonEmpty) {
      request.contentType match {
        case HttpContentTypes.JSON =>
          contentType = ContentTypes.`application/json`
          val body = request.body.find(_.contentType == HttpContentTypes.JSON)
          if (body.nonEmpty) {
            byteString = ByteString(context.renderBodyAsString(body.get.data))
          }
        case HttpContentTypes.X_WWW_FORM_URLENCODED =>
          contentType = HttpContentTypes.`x-www-form-urlencoded(UTF-8)`
          val body = request.body.find(_.contentType == HttpContentTypes.X_WWW_FORM_URLENCODED)
          if (body.nonEmpty) {
            var bodyStr: String = null
            try {
              val sb = StringBuilder.newBuilder
              val params = JacksonSupport.parse(context.renderBodyAsString(body.get.data), new TypeReference[Seq[KeyValueObject]]() {})
              for (pair <- params if pair.enabled) {
                sb.append(pair.key).append("=").append(URLEncoder.encode(pair.value, UTF8)).append("&")
              }
              if (sb.nonEmpty) {
                sb.deleteCharAt(sb.length - 1)
              }
              bodyStr = sb.toString
            } catch {
              case t: Throwable =>
                logger.warn(LogUtils.stackTraceToString(t))
                bodyStr = body.get.data
            }
            byteString = ByteString(bodyStr)
          }
        case HttpContentTypes.TEXT_PLAIN =>
          contentType = ContentTypes.`text/plain(UTF-8)`
          val body = request.body.find(_.contentType == HttpContentTypes.TEXT_PLAIN)
          if (body.nonEmpty) {
            byteString = ByteString(context.renderBodyAsString(body.get.data))
          }
        case _ =>
      }
    }
    HttpEntity(contentType, byteString)
  }
}
