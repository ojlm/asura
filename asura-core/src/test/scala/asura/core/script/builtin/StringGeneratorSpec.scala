package asura.core.script.builtin

import asura.common.ScalaTestBaseSpec
import asura.core.script.JavaScriptEngine

class StringGeneratorSpec extends ScalaTestBaseSpec {

  val engine = JavaScriptEngine.engine

  test("random") {
    val script = StringGenerator.random
    engine.eval(script)
    val value1 = engine.eval("random(5)").asInstanceOf[String]
    assertResult(5)(value1.length)
  }

  test("uuid") {
    val script = StringGenerator.UUID
    engine.eval(script)
    val value = engine.eval("uuid()").asInstanceOf[String]
    assertResult(36)(value.length)
  }

  test("UUID") {
    val script = StringGenerator.UUID
    engine.eval(script)
    val value = engine.eval("UUID.randomUUID().toString()").asInstanceOf[String]
    assertResult(36)(value.length)
  }
}
