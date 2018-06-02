package asura.core.cs.model

case class QueryCase(
                      group: String,
                      project: String,
                      api: String,
                      text: String,
                    ) extends QueryPage
