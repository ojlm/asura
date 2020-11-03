package asura.core.model

case class QuerySqlRequest(
                            var group: String,
                            var project: String,
                            host: String,
                            database: String,
                            table: String,
                            text: String,
                            sql: String,
                            hasCreators: Boolean = false,
                          ) extends QueryPage {
  var isCloned = false
}
