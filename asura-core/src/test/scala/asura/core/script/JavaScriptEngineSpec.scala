package asura.core.script

import asura.common.ScalaTestBaseSpec
import asura.core.script.builtin.StringGenerator
import javax.script.SimpleBindings

class JavaScriptEngineSpec extends ScalaTestBaseSpec {

  val engine = JavaScriptEngine.engine

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
    logger.info(fun1Result.toString)
    engine.invokeFunction("fun2", this)
  }

  test("call java function") {
    val script = StringGenerator.random
    engine.eval(script)
    logger.info(engine.eval("random(5)").toString)
  }

  test("compiled script") {
    val compiledScript = engine.compile("Math.random().toString()")
    val value = compiledScript.eval()
    compiledScript.eval()
    logger.info(value.toString)
  }

  test("binding") {
    val attributes = new java.util.HashMap[String, Object]()
    attributes.put("a", "a")
    val bindings = new SimpleBindings()
    bindings.put("attributes", attributes)
    engine.eval("print(attributes.a);attributes.a = 'b';", bindings)
    logger.info(attributes.toString)
  }
}
