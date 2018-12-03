package asura.core.cs.model

case class QueryOnlineApi(
                           domain: String,
                           method: String,
                           urlPath: String,
                           date: String,
                         ) extends QueryPage
