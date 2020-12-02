package asura.core.model

import asura.core.es.model.Label.LabelRef

case class QueryScenario(
                          group: String,
                          project: String,
                          labels: Seq[LabelRef],
                          text: String,
                          ids: Seq[String],
                        ) extends QueryPage
