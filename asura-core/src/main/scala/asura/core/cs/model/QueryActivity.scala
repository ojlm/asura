package asura.core.cs.model

case class QueryActivity(
                          group: String,
                          project: String,
                          `type`: String,
                          user: String,
                          targetId: String,
                        ) extends QueryPage
