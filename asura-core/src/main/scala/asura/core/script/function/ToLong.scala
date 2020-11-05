package asura.core.script.function

import com.fasterxml.jackson.annotation.JsonIgnore

import scala.concurrent.Future

case class ToLong() extends TransformFunction {

  @JsonIgnore
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
