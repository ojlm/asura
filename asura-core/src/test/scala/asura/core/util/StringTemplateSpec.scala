package asura.core.util

import asura.common.ScalaTestBaseSpec

class StringTemplateSpec extends ScalaTestBaseSpec {
  val map = Map("foo" -> "foo-foo", "dayName" -> "day-day")

  test("parse no tpl") {
    val tpl = "Hello"
    val res = StringTemplate.parse(tpl, map)
    assertResult(tpl)(res)
  }

  test("parse") {
    val tpl = "Hello ${foo}. Today is ${dayName}."
    val res = StringTemplate.parse(tpl, map)
    assertResult("Hello foo-foo. Today is day-day.")(res)
  }

  test("parseWithJsonPath-string") {
    val tpl = "Hello ${$.foo}. Today is ${$.dayName}."
    val json = """{"foo":"foo-foo", "dayName":"day-day"}"""
    val res = StringTemplate.parseStringWithJsonPath(tpl, json)
    assertResult("Hello foo-foo. Today is day-day.")(res)
  }

  test("parseWithJsonPath-json-provider") {
    val tpl = "Hello ${$.foo}. Today is ${$.dayName}."
    val map = Map("foo" -> "foo-foo", "dayName" -> "day-day")
    import scala.collection.JavaConverters.mapAsJavaMap
    val res = StringTemplate.parseMapWithJsonPath(tpl, mapAsJavaMap(map))
    assertResult("Hello foo-foo. Today is day-day.")(res)
  }

  test("parseWithJsonPath--path-not-found") {
    val tpl = "Hello ${$.foo-foo}. Today is ${$.dayName}."
    val json = """{"foo":"foo-foo", "dayName":"day-day"}"""
    val res = StringTemplate.parseStringWithJsonPath(tpl, json)
    println(res)
    assertResult("Hello $.foo-foo. Today is day-day.")(res)
  }

  test("parseWithJsonPath-object") {
    val tpl = "Hello ${$.foo}. Today is ${$.dayName}."
    val json = """{"foo": { "foo" :"foo-foo", "dayName" : {"dayName":"day-day"}}}"""
    val res = StringTemplate.parseStringWithJsonPath(tpl, json)
    println(res)
  }
}
