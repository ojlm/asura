package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.http.HttpContentTypes
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.{ModelImpl, RefModel, Swagger}


case class HttpRequestBody(
                            val contentType: String = StringUtils.EMPTY,
                            val jsonBody: JsonSchema = null, // application/json
                            val formBody: Seq[ParameterSchema] = null, // application/x-www-form-urlencoded
                          ) {

}

object HttpRequestBody {

  /** only json */
  def toRequestBody(openApi: Swagger, p: BodyParameter): HttpRequestBody = {
    val definitions = openApi.getDefinitions
    val schema: JsonSchema = p.getSchema match {
      case model: RefModel =>
        definitions.get(model.getSimpleRef) match {
          case model: ModelImpl =>
            JsonSchema.toJsonSchema(model)
          case _ =>
            null
        }
      case model: ModelImpl =>
        JsonSchema.toJsonSchema(model)
    }
    HttpRequestBody(contentType = HttpContentTypes.JSON, jsonBody = schema, formBody = null)
  }

  def toRequestBody(data: Seq[ParameterSchema]): HttpRequestBody = {
    if (null != data && data.nonEmpty) {
      HttpRequestBody(contentType = HttpContentTypes.X_WWW_FORM_URLENCODED, jsonBody = null, formBody = data)
    } else {
      HttpRequestBody()
    }
  }
}
