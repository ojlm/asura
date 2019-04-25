package asura.core.cs.model

case class QueryDubboRequest(
                              group: String,
                              project: String,
                              text: String,
                              interface: String,
                              method: String,
                              hasCreators: Boolean = false,
                            ) extends QueryPage
