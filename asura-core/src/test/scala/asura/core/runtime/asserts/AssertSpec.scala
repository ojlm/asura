package asura.core.runtime.asserts

import asura.common.ScalaTestBaseSpec
import asura.common.util.FutureUtils.RichFuture
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.util.{JacksonSupport, JsonPathUtils}

class AssertSpec extends ScalaTestBaseSpec {

  test("result") {
    val ctxJson =
      """
        |{
        |  "body" : {
        |    "code" : "10000",
        |    "data" : [ "one" ]
        |  }
        |}
      """.stripMargin
    val assertJson =
      """
        |{
        |  "$.body.code" : { "$eq" : "10000" },
        |  "$.body.data" : {
        |    "$type" : "array",
        |    "$size" : 1,
        |    "$.[0]" : {
        |      "$eq" : "one"
        |    }
        |  },
        |  "$type" : "map"
        |}
      """.stripMargin
    val ctx = JsonPathUtils.parse(ctxJson)
    val assert = JacksonSupport.parse(assertJson, classOf[Map[String, Any]])
    val statis = Statistic()
    AssertionContext.eval(assert, ctx, statis).await
    assertResult(true)(statis.isSuccessful)
  }
}
