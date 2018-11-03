package asura.core.cs

import java.util

import asura.common.ScalaTestBaseSpec
import asura.common.util.JsonUtils

class CaseContextSpec extends ScalaTestBaseSpec {

  val context = new util.HashMap[Any, Any]()
  context.put("a", "a")
  context.put("b", 5)
  context.put(CaseContext.KEY__P, context)

  test("miss") {
    val tpl = "hi"
    val value = CaseContext.render(tpl, context)
    assertResult(tpl)(value)
  }

  test("json-path") {
    val tpl = "{{$['b']}}"
    val value = CaseContext.render(tpl, context)
    assertResult(context.get("b"))(value)
  }

  test("random function") {
    val tpl = "{{random(5)}}"
    val value = CaseContext.render(tpl, context).asInstanceOf[String]
    assertResult(5)(value.length)
  }

  test("prev cycle reference") {
    val tpl = "{{$._p.b}}"
    val value = CaseContext.render(tpl, context)
    assertResult(context.get("b"))(value)
  }

  test("render body") {
    val tpl = "a={{$.a}}, b={{$._p.b}}"
    val result = CaseContext.renderBody(tpl, context)
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
    val str = CaseContext.renderBody(tpl, context)
    val map = JsonUtils.parse(str, classOf[Map[String, Any]])
    assertResult(map.get("a").get)("a")
    assertResult(map.get("b").get)(5)
    assertResult(map.get("random").get.asInstanceOf[String].length)(7)
  }
}
