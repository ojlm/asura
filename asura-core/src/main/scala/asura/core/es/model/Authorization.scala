package asura.core.es.model

case class Authorization(
                          val `type`: String,
                          val data: Map[String, Any]) {

}
