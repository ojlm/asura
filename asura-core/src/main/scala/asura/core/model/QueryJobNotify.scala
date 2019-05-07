package asura.core.model

case class QueryJobNotify(
                           group: String = null,
                           project: String = null,
                           jobId: String = null,
                           subscriber: String = null,
                           `type`: String = null,
                           trigger: String = null,
                           var sort: Seq[Any] = Nil,
                         ) extends QueryPage

