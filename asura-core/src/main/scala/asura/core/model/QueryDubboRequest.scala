package asura.core.model

case class QueryDubboRequest(
                              group: String,
                              project: String,
                              text: String,
                              interface: String,
                              method: String,
                              hasCreators: Boolean = false,
                            ) extends QueryPage {
  var isCloned = false
}
