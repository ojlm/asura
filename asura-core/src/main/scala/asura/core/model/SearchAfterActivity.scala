package asura.core.model

case class SearchAfterActivity(
                                `type`: String,
                                var user: String,
                                onlyMe: Boolean = false
                              ) extends SearchAfter
