package asura.core.model

case class QueryScenario(
                          group: String,
                          project: String,
                          text: String,
                          ids: Seq[String],
                        ) extends QueryPage
