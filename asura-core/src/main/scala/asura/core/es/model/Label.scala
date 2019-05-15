package asura.core.es.model

case class Label(
                  val owner: String,
                  val name: String,
                  val description: String,
                  val value: String,
                  val `type`: String
                ) {

}

object Label {

  case class LabelRef(
                       val name: String
                     ) {
  }

  object LabelType {

    val GROUP = "group"
    val PROJECT = "project"
  }

}
