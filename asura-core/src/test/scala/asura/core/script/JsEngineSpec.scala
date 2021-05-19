package asura.core.script

import asura.common.ScalaTestBaseSpec
import asura.core.script.builtin.StringGenerator

class JsEngineSpec extends ScalaTestBaseSpec {

  val engine = JsEngine.global()

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

  test("call java function") {
    val script = StringGenerator.random
    engine.eval(script)
    logger.info(engine.eval("random(5)").toString)
  }

}
