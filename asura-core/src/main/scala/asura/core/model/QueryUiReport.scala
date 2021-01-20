package asura.core.model

case class QueryUiReport(
                          var group: String,
                          var project: String,
                          `type`: String,
                          taskId: String,
                        ) extends QueryPage
