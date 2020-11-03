package asura.core.model

case class QueryEnv(
                     var group: String,
                     var project: String,
                     text: String
                   ) extends QueryPage {}
