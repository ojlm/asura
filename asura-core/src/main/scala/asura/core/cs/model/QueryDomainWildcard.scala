package asura.core.cs.model

case class QueryDomainWildcard(
                                var date: String,
                                val domain: String,
                                val tag: String,
                              ) extends QueryPage
