package asura.core.cs.assertion

import asura.common.ScalaTestBaseSpec

class RegexSpec extends ScalaTestBaseSpec {

  test("match") {
    val r = Regex("2004-01-20", raw"(\d{4})-(\d{2})-(\d{2})")
    assertResult(true)(r.isSuccessful)
  }

  test("not-match") {
    val r = Regex("2004", raw"^004")
    assertResult(false)(r.isSuccessful)
  }
}
