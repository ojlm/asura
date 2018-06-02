package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.cs.FieldTypes

/**
  * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#schemaObject
  * The Schema Object allows the definition of input and output data types.
  * These types can be objects, but also primitives and arrays.
  */
trait SchemaObject {

  var description: String

  /**
    * a [[asura.core.cs.FieldTypes]] field converted from `type` and `format`,
    */
  var `type`: String
}

object SchemaObject {

  /**
    * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#dataTypeFormat
    * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md#dataTypes
    **/
  def translateOpenApiType(t: String, format: String): String = {
    t match {
      case "string" =>
        FieldTypes.STRING
      case "integer" =>
        format match {
          case "int32" =>
            FieldTypes.INT
          case "int64" =>
            FieldTypes.LONG
          case _ =>
            FieldTypes.INT
        }
      case "number" =>
        format match {
          case "float" =>
            FieldTypes.FLOAT
          case "double" =>
            FieldTypes.DOUBLE
          case _ =>
            FieldTypes.DOUBLE
        }
      case "boolean" =>
        FieldTypes.BOOLEAN
      case "array" =>
        FieldTypes.ARRAY
      case "object" =>
        FieldTypes.MAP
      case _ =>
        StringUtils.EMPTY
    }
  }
}
