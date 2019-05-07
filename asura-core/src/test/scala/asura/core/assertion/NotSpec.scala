package asura.core.assertion

import asura.common.ScalaTestBaseSpec
import asura.common.util.FutureUtils.RichFuture
import asura.core.util.{JacksonSupport, JsonPathUtils}

class NotSpec extends ScalaTestBaseSpec {

  test("not-pass") {
    val json =
      """
        |{
        | "a" : 1,
        | "b" : 2
        |}
      """.stripMargin
    val ctx = JsonPathUtils.parse(json)
    val assertJson =
      """
        |{
        |  "$.a" : { "$eq" : 2},
        |  "$.b" : { "$eq" : 2}
        |}
      """.stripMargin
    val assert = JacksonSupport.parse(assertJson, classOf[Map[String, Any]])
    val r = Not(ctx, assert).await
    assertResult(true)(r.isSuccessful)
  }

  test("not-fail") {
    val json =
      """
        |{
        | "a" : 1,
        | "b" : 2
        |}
      """.stripMargin
    val ctx = JsonPathUtils.parse(json)
    val assertJson =
      """
        |{
        |  "$.a" : { "$eq" : 1},
        |  "$.b" : { "$eq" : 2}
        |}
      """.stripMargin
    val assert = JacksonSupport.parse(assertJson, classOf[Map[String, Any]])
    val r = Not(ctx, assert).await
    assertResult(false)(r.isSuccessful)
  }
}
