package asura.core.cs.operators

import asura.common.ScalaTestBaseSpec
import asura.core.util.JsonPathUtils

class TypeSpec extends ScalaTestBaseSpec {

  val l = 2147483648L
  val ll = 9223372036854775807L

  test("byte") {
    val json =
      """
        |{
        | "a" : 128,
        | "type" : "byte"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Type(m.get("a"), m.get("type"))
    println(r)
    assertResult(false)(r.isSuccessful)
  }

  test("float-double-lossless") {
    val json =
      """
        |{
        | "a" : 0.22222222,
        | "type" : "float"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Type(m.get("a"), m.get("type"))
    println(r)
    assertResult(true)(r.isSuccessful)
  }

  test("float-double-loss") {
    val json =
      """
        |{
        | "a" : 0.222222222,
        | "type" : "float"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Type(m.get("a"), m.get("type"))
    println(r)
    assertResult(false)(r.isSuccessful)
  }

  test("map") {
    val json =
      """
        |{
        | "a" : { "a" : "a" },
        | "type" : "map"
        |}
      """.stripMargin
    val m = JsonPathUtils.parse(json).asInstanceOf[java.util.Map[String, Object]]
    val r = Type(m.get("a"), m.get("type"))
    println(r)
    assertResult(true)(r.isSuccessful)
  }
}
