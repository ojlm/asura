package asura.core.script

import asura.common.ScalaTestBaseSpec
import javax.script.{ScriptEngine, SimpleBindings}

class BenchmarkSpec extends ScalaTestBaseSpec {

  println(s"uuid from js: ${JavaScriptEngine.eval("uuid()")}")

  val bindings = {
    val data = new java.util.HashMap[String, Any]()
    val map = new java.util.HashMap[String, Any]()
    map.put("$", data)
    data.put("a", "aa")
    map
  }

  test("uuid") {
    benchmark {
      JavaScriptEngine.eval("uuid()")
    }
  }

  test("uuid compiled") {
    val script = JavaScriptEngine.engine.compile("uuid()")
    benchmark {
      JavaScriptEngine.eval(script, null)
    }
  }

  test("js engine") {
    benchmark(testUnit(JavaScriptEngine.engine))
  }

  test("string script equal") {
    benchmark {
      JavaScriptEngine.eval("$.a==='a'", bindings).asInstanceOf[Boolean]
    }
  }

  def benchmark(func: => Any): Unit = {
    val count = 100000
    val now = System.nanoTime()
    for (i <- 1.to(count)) {
      func
    }
    val total = System.nanoTime() - now
    println(s"times: ${count}, total: ${total / 1000} ms, mean: ${total / count / 1000} ms")
  }

  def testUnit(engine: ScriptEngine): Unit = {
    val bindings = new SimpleBindings()
    val attributes = new java.util.HashMap[String, Object]()
    attributes.put("a", "a")
    bindings.put("attributes", attributes)
    val result = engine.eval("attributes.a=='a'", bindings).asInstanceOf[Boolean]
    if (!result) {
      println("false")
    }
  }
}
