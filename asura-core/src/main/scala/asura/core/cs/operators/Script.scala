package asura.core.cs.operators

import java.util

import asura.core.cs.CaseContext
import asura.core.cs.asserts.{AssertResult, FailAssertResult, PassAssertResult}
import asura.core.script.JavaScriptEngine

object Script {

  def apply(src: Any, target: Any): AssertResult = {
    if (target.isInstanceOf[String]) {
      try {
        val bindings = new util.HashMap[String, Any]()
        bindings.put(CaseContext.SELF_VARIABLE, src)
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
