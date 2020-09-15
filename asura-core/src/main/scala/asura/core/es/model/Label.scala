package asura.core.es.model

case class Label(
                  owner: String,
                  name: String,
                  description: String,
                  value: String,
                  `type`: String
                ) {

}

object Label {

  case class LabelRef(
                       name: String
                     ) {
  }

  object LabelType {

    val GROUP = "group"
    val PROJECT = "project"
  }

}
