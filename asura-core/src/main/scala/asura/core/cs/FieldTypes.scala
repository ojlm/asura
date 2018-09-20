package asura.core.cs

object FieldTypes {

  val BOOLEAN = "boolean"

  /** -128 ~ 127 */
  val BYTE = "byte"
  /** 16-bits, -32,768 ~ 32,767 */
  val SHORT = "short"
  /** 32-bits -2^31 ~ 2^31-1 */
  val INT = "int"
  /** 64-bits, -2^63 ~ 2^63-1 */
  val LONG = "long"
  val FLOAT = "float"
  val DOUBLE = "double"

  val STRING = "string"
  val ARRAY = "array"
  val MAP = "map"

  val NULL = "null"
}
