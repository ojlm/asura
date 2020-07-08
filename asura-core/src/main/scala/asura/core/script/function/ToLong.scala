package asura.core.script.function

import scala.concurrent.Future

object ToLong extends TransformFunction {

  val ZERO = java.lang.Long.valueOf(0)
  override val name: String = "toLong"
  override val description: String = "Try to transform string to a long"

  override def apply(arg: Object): Future[java.lang.Long] = {
    Future.successful {
      if (null != arg) {
        try {
          java.lang.Long.parseLong(arg.toString)
        } catch {
          case _: Throwable => ZERO
        }
      } else {
        ZERO
      }
    }
  }
}
