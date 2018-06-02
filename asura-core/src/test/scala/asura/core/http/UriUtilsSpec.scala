package asura.core.http

import asura.common.ScalaTestBaseSpec
import asura.core.http.UriUtils.mapToQueryString
import asura.core.util.JacksonSupport

class UriUtilsSpec extends ScalaTestBaseSpec {

  test("mapToQueryString-url-encode") {
    val s = """{"a": ["a", "你好"], "s":"s"}"""
    val res = JacksonSupport.parse(s, classOf[Map[String, Any]])
    val q = mapToQueryString(res, null)
    assertResult("a=a&a=%E4%BD%A0%E5%A5%BD&s=s")(q)
  }
}
