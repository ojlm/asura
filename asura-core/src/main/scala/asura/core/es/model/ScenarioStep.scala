package asura.core.es.model

case class ScenarioStep(
                         id: String,
                         `type`: String,
                         stored: Boolean = false, // do need to store in context
                         enabled: Boolean = true,
                         data: Map[String, Any] = Map.empty
                       )

object ScenarioStep {

  // use `case` for http request backward compatible
  val TYPE_HTTP = "case"
  val TYPE_SQL = "sql"
  val TYPE_DUBBO = "dubbo"
}
