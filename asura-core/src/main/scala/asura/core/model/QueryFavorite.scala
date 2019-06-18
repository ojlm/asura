package asura.core.model

case class QueryFavorite(
                          group: String,
                          project: String,
                          `type`: String,
                          user: String,
                          targetType: String,
                          targetId: String,
                          checked: String,
                        ) extends QueryPage
