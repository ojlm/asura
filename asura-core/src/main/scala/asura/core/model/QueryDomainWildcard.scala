package asura.core.model

case class QueryDomainWildcard(
                                var date: String,
                                val domain: String,
                                val tag: String,
                              ) extends QueryPage
