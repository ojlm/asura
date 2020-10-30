package asura.core.model

case class QueryPermissions(
                             var group: String,
                             var project: String,
                             var `type`: String,
                             username: String,
                           ) extends QueryPage
