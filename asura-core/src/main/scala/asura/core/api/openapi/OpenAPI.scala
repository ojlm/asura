package asura.core.api.openapi

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.model._
import asura.core.http.HttpMethods
import com.typesafe.scalalogging.Logger
import io.swagger.models.parameters.{BodyParameter, CookieParameter, FormParameter, HeaderParameter, PathParameter, QueryParameter, RefParameter, Parameter => SwaggerParameter}
import io.swagger.models.{HttpMethod, Operation, Swagger}
import io.swagger.parser.SwaggerParser

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object OpenAPI {

  val logger = Logger("OpenAPI")

  /** a limited convert */
  def v2ToApi(oasStr: String, group: String, project: String, creator: String): Seq[RestApi] = {
    val apis = ArrayBuffer[RestApi]()
    val openApi = new SwaggerParser().parse(oasStr)
    val info = openApi.getInfo
    val version = if (null != info) info.getVersion else StringUtils.EMPTY
    val basePath = if (StringUtils.isEmpty(openApi.getBasePath) || "/".equals(openApi.getBasePath)) {
      StringUtils.EMPTY
    } else {
      openApi.getBasePath
    }
    openApi.getPaths.forEach((pathStr, pathObj) => {
      val path = s"$basePath$pathStr"
      pathObj.getOperationMap.forEach((httpMethod: HttpMethod, operation: Operation) => {
        val method = httpMethod match {
          case HttpMethod.GET =>
            HttpMethods.GET
          case HttpMethod.PUT =>
            HttpMethods.PUT
          case HttpMethod.POST =>
            HttpMethods.POST
          case HttpMethod.DELETE =>
            HttpMethods.DELETE
          case HttpMethod.OPTIONS =>
            HttpMethods.OPTIONS
          case HttpMethod.HEAD =>
            HttpMethods.HEAD
          case HttpMethod.PATCH =>
            HttpMethods.PATCH
          case _ =>
            null
        }
        if (StringUtils.isNotEmpty(method)) {
          val summary = operation.getSummary
          val description = operation.getDescription
          val deprecated = operation.isDeprecated
          val pathParameters = pathObj.getParameters
          val api = RestApi(
            summary = summary,
            description = description,
            path = path,
            method = method,
            group = group,
            project = project,
            deprecated = deprecated,
            version = version,
            schema = v2OperationToSchema(openApi, operation, pathParameters.asScala.toSeq),
            creator = creator,
            createdAt = DateUtils.nowDateTime
          )
          apis += api
        }
      })
    })
    apis.toList
  }

  def v2OperationToSchema(openApi: Swagger, operation: Operation, pathParameter: Seq[SwaggerParameter]): RestApiSchema = {
    val paths = mutable.HashMap[String, ParameterSchema]()
    val query = mutable.HashMap[String, ParameterSchema]()
    val header = mutable.HashMap[String, ParameterSchema]()
    val cookie = mutable.HashMap[String, ParameterSchema]()
    val formData = mutable.HashMap[String, ParameterSchema]()
    var bodyParameter: BodyParameter = null
    val parameterHandler = (parameter: SwaggerParameter) => {
      parameter match {
        case p: PathParameter =>
          paths.put(p.getName, ParameterSchema.toParameter(p))
        case p: QueryParameter =>
          query.put(p.getName, ParameterSchema.toParameter(p))
        case p: HeaderParameter =>
          header.put(p.getName, ParameterSchema.toParameter(p))
        case p: CookieParameter =>
          cookie.put(p.getName, ParameterSchema.toParameter(p))
        case p: FormParameter =>
          formData.put(p.getName, ParameterSchema.toParameter(p))
        case p: BodyParameter =>
          bodyParameter = p
        case p: RefParameter =>
          logger.warn(s"not support: ${p}")
        case _ =>
          logger.warn(s"unknown parameter: ${parameter}")
      }
    }
    if (null != pathParameter) {
      pathParameter.foreach(parameterHandler)
    }
    val operationParameters = operation.getParameters
    if (null != operationParameters) {
      operationParameters.forEach(parameter => parameterHandler(parameter))
    }
    RestApiSchema(
      path = paths.values.toSeq,
      query = query.values.toSeq,
      header = header.values.toSeq,
      cookie = cookie.values.toSeq,
      requestBody = if (null != bodyParameter) HttpRequestBody.toRequestBody(openApi, bodyParameter) else HttpRequestBody.toRequestBody(formData.values.toSeq),
      responses = HttpResponse.toResponses(openApi, (operation.getResponses.asScala))
    )
  }
}
