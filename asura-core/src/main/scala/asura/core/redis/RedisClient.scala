package asura.core.redis

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.scalalogging.Logger
import org.redisson.Redisson
import org.redisson.api.{RFuture, RedissonClient}
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config

import scala.compat.java8.FutureConverters.{toScala => javaFutureToScalaFuture}
import scala.concurrent.Future

object RedisClient {

  val logger = Logger("RedisClient")
  var redisson: RedissonClient = null
  private val mapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)

  def init(servers: Seq[String] = Nil): Unit = {
    val config = new Config()
    config.setCodec(new JsonJacksonCodec(mapper))
    if (null == servers || servers.isEmpty) {
      config.useSingleServer().setAddress("redis://127.0.0.1:6379")
    } else if (servers.length == 1) {
      config.useSingleServer().setAddress(servers(0))
    } else {
      config.useClusterServers().setScanInterval(3000).addNodeAddress(servers: _*)
    }
    logger.info(s"init redis client with config: ${config.toJSON}")
    redisson = Redisson.create(config)
  }

  def shutdown(): Unit = {
    if (null != redisson) {
      logger.info("shutdown redis client")
      redisson.shutdown()
    }
  }

  implicit def toScala[T](rf: RFuture[T]): Future[T] = {
    javaFutureToScalaFuture(rf)
  }
}
