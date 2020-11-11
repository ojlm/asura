package asura.core.model

case class QueryJobNotify(
                           var group: String = null,
                           var project: String = null,
                           jobId: String = null,
                           subscriber: String = null,
                           `type`: String = null,
                           trigger: String = null,
                           var sort: Seq[Any] = Nil,
                         ) extends QueryPage
