package asura.core.cs.model

case class QuerySqlRequest(
                            group: String,
                            project: String,
                            text: String,
                            sql: String,
                          ) extends QueryPage
