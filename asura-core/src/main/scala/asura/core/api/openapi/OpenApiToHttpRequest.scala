package asura.core.api.openapi

import java.net.URI
import java.net.http.HttpRequest

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.Label.LabelRef
import asura.core.es.model._
import asura.core.http.{HttpContentTypes, HttpEngine2, HttpMethods}
import com.typesafe.scalalogging.Logger
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
 * v2 and v3
 */
object OpenApiToHttpRequest {

  val logger = Logger("OpenApiToHttpRequest")

  def openApiToRequest(openApiOpt: Option[OpenAPI], options: ConvertOptions): ConvertResults = {
    val apis = ArrayBuffer[HttpStepRequest]()
    var errMsg: ErrorMessage = null
    if (openApiOpt.nonEmpty) {
      try {
        val openApi = openApiOpt.get
        if (null != openApi.getServers && !openApi.getServers.isEmpty) {
          val server = openApi.getServers.get(0)
          val uri = URI.create(server.getUrl)
          val scheme = if (null != options && null != options.scheme) options.scheme else uri.getScheme
          val host = if (null != options && null != options.host) options.host else uri.getHost
          errMsg = checkBasic(scheme, host)
          if (null == errMsg && null != openApi.getPaths) {
            val basePath = if (null != options && null != options.basePath) options.basePath else {
              StringUtils.notEmptyElse(uri.getPath, StringUtils.EMPTY)
            }
            val port = if (null != options && (options.port > 0 && options.port < 65536)) {
              options.port
            } else {
              if (uri.getPort == -1) {
                scheme match {
                  case "http" => 80
                  case "https" => 443
                }
              } else {
                uri.getPort
              }
            }
            openApi.getPaths.asScala.foreach(path => buildRequest(apis, openApi, scheme, host, port, basePath, path._1, path._2, options))
          }
        }
      } catch {
        case t: Throwable => errMsg = ErrorMessages.error_Throwable(t)
      }
    }
    ConvertResults(errMsg, apis.toSeq)
  }

  def buildRequest(
                    apis: ArrayBuffer[HttpStepRequest],
                    openApi: OpenAPI,
                    scheme: String,
                    host: String,
                    port: Int,
                    basePath: String,
                    path: String,
                    pathItem: PathItem,
                    options: ConvertOptions,
                  ): Unit = {
    val urlPath = s"${basePath}${path}"
    val dealOperation = (operation: Operation, method: String) => if (null != operation) {
      var contentType = HttpContentTypes.X_WWW_FORM_URLENCODED
      val body = ArrayBuffer[MediaObject]()
      var allowEdBody = true
      val requestBody = operation.getRequestBody
      if (null != requestBody && null != requestBody.getContent && !requestBody.getContent.isEmpty) {
        val mediaOpt = requestBody.getContent.asScala.find(entry => HttpContentTypes.isSupport(entry._1))
        if (mediaOpt.nonEmpty) {
          val media = mediaOpt.get
          contentType = media._1
          val mediaType = media._2
          if (null != mediaType && null != mediaType.getSchema && null != mediaType.getSchema.get$ref() &&
            null != openApi.getComponents && null != openApi.getComponents.getSchemas) {
            val componentName = mediaType.getSchema.get$ref().substring("#/components/schemas/".length)
            val schemas = openApi.getComponents.getSchemas
            if (null != schemas && schemas.containsKey(componentName)) {
              body += MediaObject(contentType, bodySchemaToString(schemas.get(componentName)))
            } else {
              body += MediaObject(contentType, StringUtils.EMPTY)
            }
          } else {
            body += MediaObject(contentType, StringUtils.EMPTY)
          }
        } else {
          allowEdBody = false
        }
      }
      if (allowEdBody) {
        val labels = ArrayBuffer[LabelRef]()
        if (null != options && null != options.labels && options.labels.nonEmpty) {
          options.labels.foreach(label => labels += LabelRef(label))
        }
        if (null != operation.getTags) {
          operation.getTags.forEach(tag => labels += LabelRef(tag))
        }
        val summary = operation.getSummary
        val description = operation.getDescription
        val path = ArrayBuffer[KeyValueObject]()
        val query = ArrayBuffer[KeyValueObject]()
        val header = ArrayBuffer[KeyValueObject]()
        val cookie = ArrayBuffer[KeyValueObject]()
        if (null != operation.getParameters) {
          operation.getParameters.forEach(parameter => {
            parameter.getIn match {
              case "path" => path += parameterToKeyValueObject(parameter)
              case "query" => query += parameterToKeyValueObject(parameter)
              case "header" => header += parameterToKeyValueObject(parameter)
              case "cookie" => cookie += parameterToKeyValueObject(parameter)
            }
          })
        }
        val sb = new StringBuilder()
        sb.append(scheme).append("://").append(host)
        if (!((scheme == "http" && port == 80) || (scheme == "https" && port == 443))) {
          sb.append(":").append(port)
        }
        sb.append(urlPath)
        if (query.nonEmpty) {
          sb.append("?")
          query.filter(item => item.enabled).foreach(item => sb.append(item.key).append("=").append(item.value).append("&"))
          sb.deleteCharAt(sb.length() - 1)
        }
        val rawUrl = sb.toString()
        apis += HttpStepRequest(
          summary = summary,
          description = description,
          group = null,
          project = null,
          request = Request(scheme, host, rawUrl, urlPath, port, method, path.toSeq, query.toSeq, header.toSeq, cookie.toSeq, contentType, body.toSeq),
          assert = null,
          labels = labels.toSeq,
        )
      }
    }
    dealOperation(pathItem.getGet, HttpMethods.GET)
    dealOperation(pathItem.getPut, HttpMethods.PUT)
    dealOperation(pathItem.getPost, HttpMethods.POST)
    dealOperation(pathItem.getDelete, HttpMethods.DELETE)
    dealOperation(pathItem.getOptions, HttpMethods.OPTIONS)
    dealOperation(pathItem.getHead, HttpMethods.HEAD)
    dealOperation(pathItem.getPatch, HttpMethods.PATCH)
    dealOperation(pathItem.getTrace, HttpMethods.TRACE)
  }

  def parameterToKeyValueObject(parameter: Parameter): KeyValueObject = {
    val default = if (null != parameter.getSchema && null != parameter.getSchema.getDefault) {
      parameter.getSchema.getDefault.toString
    } else {
      StringUtils.EMPTY
    }
    val required: Boolean = if (null != parameter.getRequired) parameter.getRequired else false
    KeyValueObject(parameter.getName, default, required, parameter.getDescription)
  }

  def checkBasic(scheme: String, host: String): ErrorMessage = {
    if (StringUtils.isEmpty(scheme)) {
      ErrorMessages.error_EmptyProtocol
    } else if (StringUtils.isEmpty(host)) {
      ErrorMessages.error_EmptyHost
    } else {
      null
    }
  }

  def getOpenApiFromUrl(url: String): Future[Option[OpenAPI]] = {
    HttpEngine2.sendAsync(HttpRequest.newBuilder(URI.create(url)).build())
      .map(response => {
        val body = response.body()
        getOpenApi(body)
      })
  }

  def getOpenApi(content: String): Option[OpenAPI] = {
    val options = new ParseOptions()
    options.setResolve(true)
    OpenAPIV3Parser.getExtensions().asScala.map(extension => {
      val parsed = extension.readContents(content, null, options)
      parsed.getOpenAPI
    }).find(_ != null)
  }

  def fromUrl(url: String, options: ConvertOptions): Future[ConvertResults] = {
    getOpenApiFromUrl(url).map(openApi => openApiToRequest(openApi, options))
  }

  def fromContent(content: String, options: ConvertOptions): ConvertResults = {
    openApiToRequest(getOpenApi(content), options)
  }

  def bodySchemaToString[T](schema: Schema[T]): String = {
    val map = collection.mutable.HashMap[String, Any]()
    if (null != schema.getProperties) {
      schema.getProperties.forEach((k, propSchema) => {
        if (null != propSchema.getExample) {
          map.put(k, propSchema.getExample.toString)
        } else {
          map.put(k, null)
        }
      })
      JsonUtils.stringifyPretty(map)
    } else {
      StringUtils.EMPTY
    }
  }
}
