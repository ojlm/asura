package asura.core.model

case class QueryFile(
                      var group: String,
                      var project: String,
                      `type`: String,
                      parent: String,
                      name: String,
                      text: String,
                      topOnly: Boolean = true
                    ) extends QueryPage
