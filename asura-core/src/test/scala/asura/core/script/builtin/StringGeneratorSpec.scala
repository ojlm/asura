package asura.core.script.builtin

import asura.common.ScalaTestBaseSpec
import asura.core.script.JavaScriptEngine
import jdk.nashorn.api.scripting.NashornScriptEngine

class StringGeneratorSpec extends ScalaTestBaseSpec {

  val engine: NashornScriptEngine = JavaScriptEngine.engine

  test("random") {
    val script = StringGenerator.random
    engine.eval(script)
    val value1 = engine.eval("random(5)").asInstanceOf[String]
    val value2 = engine.eval("random(5)").asInstanceOf[String]
    assertResult(5)(value1.length)
    println(value1, value2)
  }

  test("uuid") {
    val script = StringGenerator.UUID
    engine.eval(script)
    val value = engine.eval("uuid()").asInstanceOf[String]
    println(value)
  }

  test("UUID") {
    val script = StringGenerator.UUID
    engine.eval(script)
    val value = engine.eval("UUID.randomUUID().toString()").asInstanceOf[String]
    println(value)
  }
}
