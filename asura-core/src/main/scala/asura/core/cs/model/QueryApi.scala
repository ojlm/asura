package asura.core.cs.model

case class QueryApi(
                     group: String,
                     project: String,
                     method: String,
                     path: String,
                     text: String
                   ) extends QueryPage {}
