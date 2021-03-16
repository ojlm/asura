package asura.core.model

case class SearchAfterLogEntry(
                                var day: String = null,
                                var reportId: String = null,
                                var levels: Seq[String] = null,
                                var `type`: Seq[String] = null,
                                var source: Seq[String] = null,
                                var hostname: Seq[String] = null,
                                var method: Seq[String] = null,
                                var desc: Boolean = true
                              ) extends SearchAfter
