package asura.core.assertion

import java.util

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}
import asura.core.runtime.RuntimeContext
import asura.core.script.JsEngine

import scala.concurrent.Future

case class Script() extends Assertion {

  override val name: String = Assertions.SCRIPT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Script.apply(actual, expect))
  }

}

object Script {

  def apply(src: Any, target: Any): AssertResult = {
    if (target.isInstanceOf[String]) {
      try {
        val bindings = new util.HashMap[String, Any]()
        bindings.put(RuntimeContext.SELF_VARIABLE, src)
        val scriptResult = JsEngine.eval(target.toString, bindings).asInstanceOf[Boolean]
        if (scriptResult) {
          PassAssertResult(1)
        } else {
          FailAssertResult(1)
        }
      } catch {
        case t: Throwable =>
          FailAssertResult(1, t.getMessage)
      }
    } else {
      FailAssertResult(1, AssertResult.msgIncomparableTargetType(target))
    }
  }

}
