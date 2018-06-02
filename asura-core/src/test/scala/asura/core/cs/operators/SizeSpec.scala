package asura.core.cs.operators

import asura.common.ScalaTestBaseSpec
import asura.core.util.JsonPathUtils

class SizeSpec extends ScalaTestBaseSpec {

  test("size-list") {
    val json =
      """
        |{
        | "a" : ["1", "2"],
        | "b" : 2
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Size(m.get("a"), m.get("b"))
    println(r)
    assertResult(true)(r.isSuccessful)
  }

  test("size-seq") {
    val r = Size(List("a", "b"), 2)
    println(r)
    assertResult(true)(r.isSuccessful)
  }

  test("size-string") {
    val r = Size("abc", 3)
    println(r)
    assertResult(true)(r.isSuccessful)
  }
}
