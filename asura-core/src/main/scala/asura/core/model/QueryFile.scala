package asura.core.model

case class QueryFile(
                      var group: String,
                      var project: String,
                      `type`: String,
                      parent: String,
                      name: String,
                    ) extends QueryPage
