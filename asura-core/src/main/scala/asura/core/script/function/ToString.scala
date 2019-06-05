package asura.core.script.function

import asura.common.util.JsonUtils

import scala.concurrent.Future

object ToString extends TransformFunction {

  override val name: String = "toString"
  override val description: String = "Transform any value to a string powered by fasterxml.jackson"

  override def apply(arg: Object): Future[String] = {
    Future.successful {
      if (null != arg) {
        JsonUtils.stringify(arg)
      } else {
        "null"
      }
    }
  }
}
