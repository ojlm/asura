package asura.core.sql.actor

import java.sql.Connection

import akka.actor.Props
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.common.util.FutureUtils
import asura.core.CoreConfig
import asura.core.es.model.SqlRequest
import asura.core.sql.actor.MySqlConnectionCacheActor.GetConnectionMessage
import asura.core.sql.{MySqlConnector, SqlConfig, SqlParserUtils, SqlResult}

import scala.concurrent.{ExecutionContext, Future}

class SqlRequestInvokerActor extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT

  val connectionCacheActor = context.actorOf(MySqlConnectionCacheActor.props())

  override def receive: Receive = {
    case sqlRequest: SqlRequest =>
      getResponse(sqlRequest) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def getResponse(sqlRequest: SqlRequest): Future[SqlResult] = {
    implicit val sqlEc = SqlConfig.SQL_EC
    val futConn = (connectionCacheActor ? GetConnectionMessage(sqlRequest)).asInstanceOf[Future[Connection]]
    val (isOk, errMsg) = SqlParserUtils.isSelectStatement(sqlRequest.sql)
    if (null == errMsg) {
      futConn.flatMap(conn => {
        if (isOk) {
          Future {
            MySqlConnector.executeQuery(conn, sqlRequest.sql)
          }
        } else {
          Future {
            MySqlConnector.executeUpdate(conn, sqlRequest.sql)
          }
        }
      }).flatMap(result => {
        SqlResult.evaluate(sqlRequest, result)
      })
    } else {
      FutureUtils.requestFail(errMsg)
    }
  }

}

object SqlRequestInvokerActor {

  def props() = Props(new SqlRequestInvokerActor())
}
