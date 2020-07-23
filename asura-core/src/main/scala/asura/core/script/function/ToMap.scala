package asura.core.script.function

import asura.core.util.JsonPathUtils

import scala.concurrent.Future

case class ToMap() extends TransformFunction {

  override val name: String = "toMap"
  override val description: String = "Try to transform string to a map powered by fasterxml.jackson"

  override def apply(arg: Object): Future[java.util.Map[Object, Object]] = {
    Future.successful {
      if (null != arg) {
        try {
          JsonPathUtils.parse(arg.asInstanceOf[String]).asInstanceOf[java.util.Map[Object, Object]]
        } catch {
          case t: Throwable => java.util.Collections.singletonMap("error", t.getMessage)
        }
      } else {
        java.util.Collections.EMPTY_MAP.asInstanceOf[java.util.Map[Object, Object]]
      }
    }
  }
}
