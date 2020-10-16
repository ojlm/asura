package asura.core.http

import java.net.URLEncoder

import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import asura.common.util.{LogUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.es.model.{FormDataItem, HttpCaseRequest, KeyValueObject, MediaObject}
import asura.core.http.UriUtils.UTF8
import asura.core.runtime.RuntimeContext
import asura.core.store.BlobStoreEngines
import asura.core.util.JacksonSupport
import com.fasterxml.jackson.core.`type`.TypeReference
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.concurrent.Future

object EntityUtils {

  val logger = Logger("EntityUtils")
  val EMPTY_ENTITY = HttpEntity(ContentTypes.NoContentType, ByteString.empty)

  def toEntity(cs: HttpCaseRequest, context: RuntimeContext): Future[RequestEntity] = {
    val request = cs.request
    if (StringUtils.isNotEmpty(request.contentType) && null != request.body && request.body.nonEmpty) {
      request.contentType match {
        case HttpContentTypes.JSON =>
          val body = request.body.find(_.contentType == HttpContentTypes.JSON)
          Future.successful(HttpEntity(
            ContentTypes.`application/json`,
            if (body.nonEmpty) ByteString(context.renderTemplateAsString(body.get.data)) else ByteString.empty
          ))
        case HttpContentTypes.X_WWW_FORM_URLENCODED =>
          val contentType = ContentTypes.`application/x-www-form-urlencoded`
          val body = request.body.find(_.contentType == HttpContentTypes.X_WWW_FORM_URLENCODED)
          val data = buildFormUrlBodyString(body, context)
          Future.successful(HttpEntity(contentType, data))
        case HttpContentTypes.TEXT_PLAIN =>
          val body = request.body.find(_.contentType == HttpContentTypes.TEXT_PLAIN)
          Future.successful(HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            if (body.nonEmpty) ByteString(context.renderTemplateAsString(body.get.data)) else ByteString.empty
          ))
        case HttpContentTypes.MULTIPART_FORM_DATA =>
          val body = request.body.find(_.contentType == HttpContentTypes.MULTIPART_FORM_DATA)
          buildMultipartForm(body, context)
        case _ =>
          Future.successful(EMPTY_ENTITY)
      }
    } else {
      Future.successful(EMPTY_ENTITY)
    }
  }

  private def buildMultipartForm(body: Option[MediaObject], context: RuntimeContext): Future[RequestEntity] = {
    import asura.core.concurrent.ExecutionContextManager.cachedExecutor
    if (body.nonEmpty) {
      try {
        val params = JacksonSupport.parse(body.get.data, new TypeReference[Seq[FormDataItem]]() {})
        val parts = mutable.ArrayBuffer[Multipart.FormData.BodyPart]()
        val futureParts = params.filter(param => param.enabled && StringUtils.isNotEmpty(param.key))
          .foldLeft(Future.successful(parts))((futureParts, param) => {
            for {
              parts <- futureParts
              next <- {
                param.`type` match {
                  case FormDataItem.TYPE_BLOB =>
                    val metaData = param.metaData
                    if (null != metaData) {
                      val engine = BlobStoreEngines.get(metaData.engine)
                      if (engine.nonEmpty) {
                        engine.get.download(metaData.key).map(downloadParams => {
                          parts += Multipart.FormData.BodyPart(
                            param.key,
                            HttpEntity(
                              MediaTypes.`application/octet-stream`,
                              downloadParams.length.getOrElse(0), downloadParams.source
                            ),
                            _additionalDispositionParams = Map("filename" -> metaData.fileName)
                          )
                          parts
                        })
                      } else {
                        ErrorMessages.error_StoreEngineNotAvailable(metaData.engine).toFutureFail
                      }
                    } else {
                      Future.successful(parts)
                    }
                  case _ => // FormDataItem.TYPE_STRING or empty
                    val rendered = context.renderTemplateAsString(param.value)
                    parts += Multipart.FormData.BodyPart.Strict(param.key, rendered)
                    Future.successful(parts)
                }
              }
            } yield next
          })
        futureParts.map(parts => Multipart.FormData(Source(parts.toVector)).toEntity)
      } catch {
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          throw ErrorMessages.error_Throwable(t).toException
      }
    } else {
      Future.successful(EMPTY_ENTITY)
    }
  }

  private def buildFormUrlBodyString(body: Option[MediaObject], context: RuntimeContext): ByteString = {
    if (body.nonEmpty) {
      try {
        val sb = new StringBuilder()
        val params = JacksonSupport.parse(body.get.data, new TypeReference[Seq[KeyValueObject]]() {})
        for (pair <- params if (pair.enabled && StringUtils.isNotEmpty(pair.key))) {
          val rendered = context.renderTemplateAsString(pair.value)
          sb.append(pair.key).append("=").append(URLEncoder.encode(rendered, UTF8)).append("&")
        }
        if (sb.nonEmpty) {
          sb.deleteCharAt(sb.length - 1)
        }
        ByteString(sb.toString)
      } catch {
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          throw ErrorMessages.error_Throwable(t).toException
      }
    } else {
      ByteString.empty
    }
  }
}
