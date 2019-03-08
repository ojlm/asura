package asura.core.es.model

import scala.collection.mutable

case class Authorization(
                          val `type`: String,
                          val data: mutable.Map[String, Any]) {

}
