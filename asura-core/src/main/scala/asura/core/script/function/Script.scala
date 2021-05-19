package asura.core.script.function

import asura.common.util.StringUtils
import asura.core.runtime.RuntimeContext.SELF_VARIABLE
import asura.core.script.JsEngine

import scala.concurrent.Future

case class Script() extends TransformFunction {

  override val name: String = "script"
  override val description: String = "Run a script and extract the result"

  override def apply(arg: Object): Future[Object] = {
    Future.successful {
      val realArg = arg.asInstanceOf[ArgWithExtraData]
      if (null != realArg.value && null != realArg.extra && StringUtils.isNotEmpty(realArg.extra.script)) {
        val bindings = new java.util.HashMap[String, Any]()
        bindings.put(SELF_VARIABLE, realArg.value)
        JsEngine.eval(realArg.extra.script, bindings).asInstanceOf[AnyRef]
      } else {
        "Illegal script argument"
      }
    }
  }
}
