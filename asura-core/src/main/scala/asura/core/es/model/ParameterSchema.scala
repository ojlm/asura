package asura.core.es.model

import asura.core.cs.FieldTypes
import asura.core.es.model.ParameterSchema.ArrayItems
import io.swagger.models.parameters.{AbstractSerializableParameter, Parameter => SwaggerParameter}

import scala.collection.mutable

case class ParameterSchema(
                            var name: String,
                            var description: String,
                            var `type`: String,
                          ) extends SchemaObject {

  /**
    * Sets the ability to pass empty-valued parameters.
    * This is valid only for either query or formData parameters and
    * allows you to send a parameter with a name only or an empty value.
    * Default value is false.
    */
  var allowEmptyValue: Boolean = true

  /**
    * for number fields
    */
  var maximum: BigDecimal = -1
  var exclusiveMaximum: Boolean = false
  var minimum: BigDecimal = -1
  var exclusiveMinimum: Boolean = false

  /**
    * for `string` and `array` type, union of `maxLength` and `maxItems`
    */
  var maxSize: Int = -1
  var minSize: Int = -1

  /**
    * `type` is `string`
    * This string should be a valid regular expression
    */
  var pattern: String = null

  var enum: Set[Any] = null

  /**
    * Required if type is `array`. Describes the type of items in the array.
    * Not in `body` or  in `formData` only support basic types, string、numbers、boolean, will only the `type` field is significant
    */
  var items: ArrayItems = null

  /**
    * Not in `body`, Required if type is `array`.
    *
    * Determines the format of the array if type array is used. Possible values are:
    * csv - comma separated values foo,bar.
    * ssv - space separated values foo bar.
    * tsv - tab separated values foo\tbar.
    * pipes - pipe separated values foo|bar.
    * Default value is csv.
    */
  var collectionFormat: String = null

  /**
    * Required if type is `object` and in is `body`.
    */
  var properties: Map[String, SchemaObject] = null

  /**
    * Determines whether this parameter is mandatory.
    * If the parameter is in "path", this property is required and its value MUST be true.
    * Otherwise, the property MAY be included and its default value is false.
    */
  var required: Boolean = false
}

object ParameterSchema {

  case class ArrayItems(description: String, `type`: String)

  object InType {

    val PATH = "path"
    val QUERY = "query"
    val HEADER = "header"
    /** v3 */
    val COOKIE = "cookie"
    /** v2 */
    val FORM_DATA = "formData"
    /** v2 */
    val BODY = "body"
  }

  def toParameter[T <: AbstractSerializableParameter[T]](p: AbstractSerializableParameter[T]): ParameterSchema = {
    val parameter = ParameterSchema(name = p.getName, description = p.getDescription, `type` = SchemaObject.translateOpenApiType(p.getType, p.getFormat))
    parameter.required = p.getRequired
    if (null != p.getMinimum) {
      parameter.minimum = p.getMinimum
    }
    if (null != p.isExclusiveMinimum) {
      parameter.exclusiveMinimum = p.isExclusiveMinimum
    }
    if (null != p.getMaximum) {
      parameter.maximum = p.getMaximum
    }
    if (null != p.isExclusiveMaximum) {
      parameter.exclusiveMaximum = p.isExclusiveMaximum
    }
    if (null != p.getEnum) {
      val enumSet = mutable.Set[Any]()
      p.getEnumValue.forEach(v => enumSet.add(v))
      parameter.enum = enumSet.toSet
    }
    if (FieldTypes.ARRAY.equals(parameter.`type`)) {
      if (null != p.getMaxItems) {
        parameter.maxSize = p.getMaxItems
      }
      if (null != p.getMinItems) {
        parameter.minSize = p.getMinItems
      }
      parameter.collectionFormat = p.getCollectionFormat
      val items = p.getItems
      if (null != items) {
        parameter.items = ArrayItems(description = items.getDescription, `type` = SchemaObject.translateOpenApiType(items.getType, items.getFormat))
      }
    }
    if (FieldTypes.STRING.equals(parameter.`type`)) {
      if (null != p.getMaxLength) {
        parameter.maxSize = p.getMaxLength
      }
      if (null != p.getMinLength) {
        parameter.minSize = p.getMinLength
      }
      if (null != p.getPattern) {
        parameter.pattern = p.getPattern
      }
    }
    parameter
  }

}
