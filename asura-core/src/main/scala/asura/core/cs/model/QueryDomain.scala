package asura.core.cs.model

case class QueryDomain(
                        names: Seq[String],
                        var date: String,
                      ) extends QueryPage
