package asura.core.cs.model

case class QueryJobNotify(
                           group: String = null,
                           project: String = null,
                           jobId: String = null,
                           subscriber: String = null,
                           `type`: String = null,
                           trigger: String = null,
                         ) extends QueryPage

