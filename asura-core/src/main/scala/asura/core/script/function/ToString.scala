package asura.core.script.function

import asura.common.util.JsonUtils

import scala.concurrent.Future

object ToString extends TransformFunction {

  override val name: String = "toString"

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
