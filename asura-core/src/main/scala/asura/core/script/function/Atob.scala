package asura.core.script.function

import java.util.Base64

import scala.concurrent.Future

object Atob extends TransformFunction {

  override val name: String = "atob"
  override val description: String = "Decodes a string of data which has been encoded using base-64 encoding"

  override def apply(arg: Object): Future[String] = {
    Future.successful {
      if (null != arg) {
        new String(Base64.getDecoder().decode(arg.toString))
      } else {
        "null"
      }
    }
  }
}
