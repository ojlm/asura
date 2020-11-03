package asura.core.model

case class QueryCase(
                      var group: String,
                      var project: String,
                      host: String,
                      path: String,
                      methods: Seq[String],
                      text: String,
                      ids: Seq[String],
                      labels: Seq[String],
                      hasCreators: Boolean = false,
                    ) extends QueryPage {
  var isCloned = false
}
