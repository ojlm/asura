package asura.core.assertion

import java.util

import asura.core.runtime.RuntimeContext
import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}
import asura.core.script.JavaScriptEngine

import scala.concurrent.Future

object Script extends Assertion {

  override val name: String = Assertions.SCRIPT

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(apply(actual, expect))
  }

  def apply(src: Any, target: Any): AssertResult = {
    if (target.isInstanceOf[String]) {
      try {
        val bindings = new util.HashMap[String, Any]()
        bindings.put(RuntimeContext.SELF_VARIABLE, src)
        val scriptResult = JavaScriptEngine.eval(target.toString, bindings).asInstanceOf[Boolean]
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
