package asura.core.model

case class QueryCiEvents(
                          group: String,
                          project: String,
                          env: String,
                          `type`: String,
                          service: String,
                        ) extends QueryPage
