package asura.core.assertion

import asura.common.ScalaTestBaseSpec
import asura.common.util.FutureUtils.RichFuture
import asura.core.util.{JacksonSupport, JsonPathUtils}

class NorSpec extends ScalaTestBaseSpec {

  test("nor-pass") {
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
        |  { "$.b" : { "$eq" : 1} }
        |]
      """.stripMargin
    val r = Nor(ctx, JacksonSupport.parse(asserts, classOf[List[Any]])).await
    assertResult(true)(r.isSuccessful)
  }

  test("nor-fail") {
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
        |  { "$.a" : { "$eq" : 1} },
        |  { "$.b" : { "$eq" : 2} }
        |]
      """.stripMargin
    val r = Nor(ctx, JacksonSupport.parse(asserts, classOf[List[Any]])).await
    assertResult(false)(r.isSuccessful)
  }
}
