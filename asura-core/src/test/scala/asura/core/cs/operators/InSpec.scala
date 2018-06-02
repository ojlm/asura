package asura.core.cs.operators

import asura.common.ScalaTestBaseSpec
import asura.core.util.JsonPathUtils

class InSpec extends ScalaTestBaseSpec {

  test("in-list") {
    val json =
      """
        |{
        | "a" : [ "a", "ab" ],
        | "b" : "a"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = In(m.get("a"), m.get("b"))
    println(r)
    assertResult(true)(r.isSuccessful)
  }

  test("in-string") {
    val r = In("abc", "bc")
    println(r)
    assertResult(true)(r.isSuccessful)
  }

  test("null") {
    val json =
      """
        |{
        | "b" : "a"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = In(m.get("a"), m.get("b"))
    println(r)
    assertResult(false)(r.isSuccessful)
  }

  test("incompatible-type") {
    val json =
      """
        |{
        | "a" : { "a": "ab" },
        | "b" : "a"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = In(m.get("a"), m.get("b"))
    println(r)
    assertResult(false)(r.isSuccessful)
  }

  test("Scala List") {
    val r = In(List(1, 2), 1)
    println(r)
    assertResult(true)(r.isSuccessful)
  }
}
