package asura.core.cs.model

case class QueryJobReport(
                           scheduler: String,
                           group: String,
                           classAlias: String,
                           `type`: String,
                         ) extends QueryPage
