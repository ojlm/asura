package asura.core.cs.model

case class AggsItem(
                     var `type`: String,
                     var id: String,
                     var count: Long,
                     var sub: Seq[AggsItem] = null,
                     var summary: String = null,
                     var description: String = null,
                   ) {

}
