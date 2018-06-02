package asura.core.script

import asura.common.ScalaTestBaseSpec
import asura.core.script.builtin.StringGenerator
import javax.script.{ScriptEngineManager, SimpleBindings}
import jdk.nashorn.api.scripting.NashornScriptEngine

class JavaScriptEngineSpec extends ScalaTestBaseSpec {

  val sem = new ScriptEngineManager()
  val engine: NashornScriptEngine = sem.getEngineByName("javascript").asInstanceOf[NashornScriptEngine]

  test("hello coco") {
    val name = "coco"
    engine.eval(s"print('hello $name')")
  }

  test("return int") {
    val value: Int = engine.eval("1 + 3").asInstanceOf[Int]
    assertResult(4)(value)
  }

  test("call function") {
    val script =
      """
        |function sayHi(name) {
        | print('hi ' + name);
        |}
      """.stripMargin
    engine.eval(script)
    engine.eval("sayHi('coco');")
  }

  test("use java type") {
    val script =
      """
        |var Thread = Java.type("java.lang.Thread");
        |
        |var MyThread = Java.extend(Thread, {
        |    run: function() {
        |        print("Run in separate thread");
        |    }
        |});
        |var th = new MyThread();
        |th.start();
        |th.join();
      """.stripMargin
    engine.eval(script)
  }

  test("call js function from java") {
    val script =
      """
        |var fun1 = function(name) {
        |    print('Hi there from Javascript, ' + name);
        |    return "greetings from javascript";
        |};
        |
        |var fun2 = function (object) {
        |    print("JS Class Definition: " + Object.prototype.toString.call(object));
        |};
      """.stripMargin
    engine.eval(script)
    val fun1Result = engine.invokeFunction("fun1", "coco")
    println(fun1Result)
    engine.invokeFunction("fun2", this)
  }

  test("call java function") {
    val script = StringGenerator.random
    engine.eval(script)
    println(engine.eval("random(5)"))
  }

  test("compiled script") {
    val compiledScript = engine.compile("Math.random().toString()")
    val value = compiledScript.eval()
    compiledScript.eval()
    println(value)
  }

  test("binding") {
    val attributes = new java.util.HashMap[String, Object]()
    attributes.put("a", "a")
    val bindings = new SimpleBindings()
    bindings.put("attributes", attributes)
    engine.eval("print(attributes.a);attributes.a = 'b';", bindings)
    println(attributes)
  }
}
