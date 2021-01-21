package asura.core.model

case class SearchAfterLogEntry(
                                day: String,
                                var reportId: String,
                                levels: Seq[String],
                              ) extends SearchAfter
