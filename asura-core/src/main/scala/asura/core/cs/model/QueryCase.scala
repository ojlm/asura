package asura.core.cs.model

case class QueryCase(
                      group: String,
                      project: String,
                      path: String,
                      method: String,
                      text: String,
                      ids: Seq[String],
                      label: String,
                    ) extends QueryPage
