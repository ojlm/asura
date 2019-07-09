package asura.core.script.function

import java.nio.charset.StandardCharsets
import java.util.Base64

import scala.concurrent.Future

object Btoa extends TransformFunction {

  override val name: String = "btoa"
  override val description: String = "Creates a base-64 encoded ASCII string from a string"

  override def apply(arg: Object): Future[String] = {
    Future.successful {
      if (null != arg) {
        Base64.getEncoder().encodeToString(arg.asInstanceOf[String].getBytes(StandardCharsets.UTF_8))
      } else {
        "null"
      }
    }
  }
}
