package asura.core.es.model

import asura.core.http.HttpContentTypes
import io.swagger.models.properties.RefProperty
import io.swagger.models.{ModelImpl, Response, Swagger}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class HttpResponse(
                         description: String,
                         headers: Seq[ParameterSchema],
                         contentType: String,
                         schema: JsonSchema
                       ) {

}

object HttpResponse {

  /** json only */
  def toResponses(openApi: Swagger, responses: mutable.Map[String, Response]): Map[String, HttpResponse] = {
    val definitions = openApi.getDefinitions
    val responseMap = mutable.Map[String, HttpResponse]()
    for ((code, res) <- responses) {
      val schema: JsonSchema = res.getSchema match {
        case p: RefProperty =>
          definitions.get(p.getSimpleRef) match {
            case model: ModelImpl =>
              JsonSchema.toJsonSchema(model)
            case _ =>
              null
          }
        case _ =>
          null
      }
      val headers = ArrayBuffer[ParameterSchema]()
      if (null != res.getHeaders) {
        res.getHeaders.forEach((name, property) => {
          headers += (ParameterSchema(
            name = name,
            description = property.getDescription,
            `type` = SchemaObject.translateOpenApiType(property.getType, property.getFormat)
          ))
        })
      }
      responseMap += (code -> HttpResponse(
        description = res.getDescription,
        headers = headers.toList,
        contentType = HttpContentTypes.JSON,
        schema = schema
      ))
    }
    responseMap.toMap
  }
}
