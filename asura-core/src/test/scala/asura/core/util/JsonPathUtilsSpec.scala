package asura.core.util

import asura.common.ScalaTestBaseSpec

class JsonPathUtilsSpec extends ScalaTestBaseSpec {

  test("parse") {
    val json = """{"a":"a", "b": {"name" : "b"}}"""
    val ref = JsonPathUtils.parse(json)
    println(ref)
  }
}
