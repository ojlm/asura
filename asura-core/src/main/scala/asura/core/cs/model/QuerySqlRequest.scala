package asura.core.cs.model

case class QuerySqlRequest(
                            group: String,
                            project: String,
                            host: String,
                            database: String,
                            table: String,
                            text: String,
                            sql: String,
                          ) extends QueryPage
