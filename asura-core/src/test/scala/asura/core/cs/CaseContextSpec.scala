package asura.core.cs

import java.util

import asura.common.ScalaTestBaseSpec

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
    println(value)
    assertResult(5)(value.length)
  }

  test("uuid function") {
    val tpl = "{{uuid()}}"
    val value = CaseContext.render(tpl, context).asInstanceOf[String]
    println(value)
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
    val result = CaseContext.renderBody(tpl, context)
    println(result)
  }
}
