package asura.core.cs.assertion

import asura.common.ScalaTestBaseSpec
import asura.core.util.{JacksonSupport, JsonPathUtils}

class AndSpec extends ScalaTestBaseSpec {

  test("and-fail-once") {
    val json =
      """
        |{
        | "a" : 1,
        | "b" : 2
        |}
      """.stripMargin
    val ctx = JsonPathUtils.parse(json)
    val asserts =
      """
        |[
        |  { "$.a" : { "$eq" : 2} },
        |  { "$.b" : { "$eq" : 2} }
        |]
      """.stripMargin
    val r = And(ctx, JacksonSupport.parse(asserts, classOf[List[Any]]))
    println(r)
    assertResult(false)(r.isSuccessful)
  }
}
