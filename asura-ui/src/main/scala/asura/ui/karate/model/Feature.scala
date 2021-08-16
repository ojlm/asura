package asura.ui.karate.model

case class Feature(
                    name: String,
                    description: String,
                    background: Int,
                    scenarios: Seq[Scenario],
                  )
