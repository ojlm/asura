package asura.core.cs.model

case class QueryJobReport(
                           scheduler: String,
                           group: String,
                           project: String,
                           classAlias: String,
                           text: String,
                           `type`: String,
                         ) extends QueryPage
