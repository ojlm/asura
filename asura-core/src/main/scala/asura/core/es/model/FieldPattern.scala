package asura.core.es.model

case class FieldPattern(
                         field: String,
                         value: String,
                         alias: String,
                         `type`: String,
                       )

object FieldPattern {

  val TYPE_TERM = "term"
  val TYPE_WILDCARD = "wildcard"
  val TYPE_REGEX = "regex"
}
