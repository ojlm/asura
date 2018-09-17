package asura.core.es.model

case class ScenarioStep(
                         id: String,
                         `type`: String,
                         data: Map[String, Any] = Map.empty
                       )

object ScenarioStep {

  val TYPE_CASE = "case"
}
