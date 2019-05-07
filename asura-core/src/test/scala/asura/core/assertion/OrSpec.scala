package asura.core.assertion

import asura.common.ScalaTestBaseSpec
import asura.common.util.FutureUtils.RichFuture
import asura.core.util.{JacksonSupport, JsonPathUtils}

class OrSpec extends ScalaTestBaseSpec {

  test("or-pass") {
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
    val r = Or(ctx, JacksonSupport.parse(asserts, classOf[List[Any]])).await
    assertResult(true)(r.isSuccessful)
    assertResult(1)(r.passed)
    assertResult(1)(r.failed)
  }
}
