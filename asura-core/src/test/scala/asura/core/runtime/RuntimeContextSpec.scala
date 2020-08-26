package asura.core.runtime

import java.util

import asura.common.ScalaTestBaseSpec
import asura.common.util.JsonUtils

class RuntimeContextSpec extends ScalaTestBaseSpec {

  val context = new util.HashMap[Any, Any]()
  context.put("a", "a")
  context.put("b", 5)

  test("miss") {
    val tpl = "hi"
    val value = RuntimeContext.renderSingleMacro(tpl, context)
    assertResult(tpl)(value)
  }

  test("json-path") {
    val tpl = "{{$['b']}}"
    val value = RuntimeContext.renderSingleMacro(tpl, context)
    assertResult(context.get("b"))(value)
  }

  test("random function") {
    val tpl = "{{random(5)}}"
    val value = RuntimeContext.renderSingleMacro(tpl, context).asInstanceOf[String]
    assertResult(5)(value.length)
  }

  test("prev cycle reference") {
    val tpl = "{{$._p.b}}"
    val value = RuntimeContext.renderSingleMacro(tpl, context)
    assertResult(context.get("b"))(value)
  }

  test("render body") {
    val tpl = "a={{$.a}}, b={{$._p.b}}"
    val result = RuntimeContext.renderTemplate(tpl, context)
    assertResult("a=a, b=5")(result)
  }

  test("render json body") {
    val tpl =
      """
        |{
        |  "a" : "{{$.a}}",
        |  "b" : {{$.b}},
        |  "random" : "{{random(7)}}",
        |  "uuid" : "{{uuid()}}"
        |}
      """.stripMargin
    val str = RuntimeContext.renderTemplate(tpl, context)
    val map = JsonUtils.parse(str, classOf[Map[String, Any]])
    assertResult(map.get("a").get)("a")
    assertResult(map.get("b").get)(5)
    assertResult(map.get("random").get.asInstanceOf[String].length)(7)
  }
}
