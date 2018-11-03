package asura.core.redis

import asura.common.ScalaTestBaseSpec

case class Hello(msg: String, ok: Boolean)

class RedisClientSpec extends ScalaTestBaseSpec {

  //  RedisClient.init()
  //
  //  test("set") {
  //    val bucket = redisson.getBucket[Map[String, String]]("map")
  //    bucket.set(Map("a" -> "a", "b" -> "b"))
  //  }
  //
  //  test("get") {
  //    val bucket = redisson.getBucket[Map[String, String]]("map")
  //    val map = bucket.get()
  //    println(map)
  //  }
  //
  //  test("set-async") {
  //    val a = redisson.getBucket[String]("a")
  //    val future = a.setAsync("a")
  //    DebugUtils.printFuture(future)
  //  }
  //
  //  test("get-async") {
  //    val a = redisson.getBucket[String]("a")
  //    val future = a.getAsync()
  //    DebugUtils.printFuture(future)
  //  }
  //
  //  test("set-obj") {
  //    val bucket = redisson.getBucket[Hello]("hello")
  //    bucket.set(Hello("world", true))
  //  }
  //
  //  test("get-obj") {
  //    val bucket = redisson.getBucket[Hello]("hello")
  //    val hello = bucket.get()
  //    println(hello)
  //  }
  //
  //  test("multi-get") {
  //    val buckets = redisson.getBuckets(StringCodec.INSTANCE)
  //    val result = buckets.get[String]("a", "b")
  //    println(result)
  //  }
  //
  //  test("hash-get") {
  //    val hash = redisson.getMap[String, String]("hash", StringCodec.INSTANCE)
  //    val value = hash.getAll(Set("a", "b").asJava)
  //    println(value)
  //  }
}
