package asura.core.cs.assertion

import asura.common.ScalaTestBaseSpec
import asura.core.util.JsonPathUtils

class GtSpec extends ScalaTestBaseSpec {

  test("null") {
    val json = "{}"
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(false)(r.isSuccessful)
  }

  test("int") {
    val json =
      """
        |{
        | "a" : 2,
        | "b" : 1
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(true)(r.isSuccessful)
  }

  test("string") {
    val json =
      """
        |{
        | "a" : "abb",
        | "b" : "bac"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(false)(r.isSuccessful)
  }

  test("float-double") {
    val json =
      """
        |{
        | "a" : 3.4,
        | "b" : 5.23
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(false)(r.isSuccessful)
  }

  test("fail-not-the-same-type") {
    val json =
      """
        |{
        | "a" : 3,
        | "b" : 5.23
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(false)(r.isSuccessful)
  }

  test("fail-incomparable-type") {
    val json =
      """
        |{
        | "a" : { "aa": "aa" },
        | "b" : { "bb": "bb" }
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Gt(m.get("a"), m.get("b"))
    assertResult(false)(r.isSuccessful)
  }
}
