package asura.core.sql.actor

import java.sql.Connection

import akka.actor.Props
import akka.pattern.pipe
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.common.cache.LRUCache
import asura.core.CoreConfig
import asura.core.es.model.SqlRequest.SqlRequestBody
import asura.core.sql.actor.MySqlConnectionCacheActor.GetConnectionMessage
import asura.core.sql.{MySqlConnector, SqlConfig}

import scala.concurrent.{ExecutionContext, Future}

class MySqlConnectionCacheActor(size: Int) extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT

  private val lruCache = LRUCache[String, Connection](size, (_, conn) => {
    conn.close()
  })

  override def receive: Receive = {
    case GetConnectionMessage(sqlRequest) =>
      getConnection(sqlRequest) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  private def getConnection(request: SqlRequestBody): Future[Connection] = {
    Future {
      val key = generateCacheKey(request)
      val conn = lruCache.get(key)
      if (null == conn || !conn.isValid(SqlConfig.SQL_CONN_CHECK_TIMEOUT)) {
        val newConn = MySqlConnector.connect(request)
        lruCache.put(key, newConn)
        newConn
      } else {
        conn
      }
    }(SqlConfig.SQL_EC)
  }

  private def generateCacheKey(request: SqlRequestBody): String = {
    val sb = new StringBuilder()
    sb.append(request.username).append(":")
      .append(request.encryptedPass).append("@")
      .append(request.host).append(":")
      .append(request.port).append("/")
      .append(request.database)
    sb.toString()
  }
}

object MySqlConnectionCacheActor {

  def props(size: Int = SqlConfig.DEFAULT_MYSQL_CONNECTOR_CACHE_SIZE) = Props(new MySqlConnectionCacheActor(size))

  case class GetConnectionMessage(request: SqlRequestBody)

}
