package asura.core.cs.asserts

import asura.common.ScalaTestBaseSpec
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
    println(ctx)
    val assert = JacksonSupport.parse(assertJson, classOf[Map[String, Any]])
    println(assert)
    val statis = Statistic()
    val result = Assert(assert, ctx, statis).result
    println("\nresult ===>")
    println(result)
    println(s"\nstatis(${statis.total}, success: ${statis.isSuccessful}) ===>")
    println(statis)
  }
}
