package asura.core.sql.actor

import java.sql.Connection

import akka.actor.Props
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.common.util.FutureUtils
import asura.core.CoreConfig
import asura.core.es.model.SqlRequest.SqlRequestBody
import asura.core.sql.actor.MySqlConnectionCacheActor.GetConnectionMessage
import asura.core.sql.{MySqlConnector, SqlConfig, SqlParserUtils}

import scala.concurrent.{ExecutionContext, Future}

class SqlRequestInvokerActor extends BaseActor {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT

  val connectionCacheActor = context.actorOf(MySqlConnectionCacheActor.props())

  override def receive: Receive = {
    case requestBody: SqlRequestBody =>
      getResponse(requestBody) pipeTo sender()
    case _ =>
      Future.failed(new RuntimeException("Unknown message type")) pipeTo sender()
  }

  def getResponse(requestBody: SqlRequestBody): Future[Object] = {
    implicit val sqlEc = SqlConfig.SQL_EC
    val futConn = (connectionCacheActor ? GetConnectionMessage(requestBody)).asInstanceOf[Future[Connection]]
    val (isOk, errMsg) = SqlParserUtils.isSelectStatement(requestBody.sql)
    if (null == errMsg) {
      futConn.flatMap(conn => {
        if (isOk) {
          Future {
            MySqlConnector.executeQuery(conn, requestBody.sql)
          }
        } else {
          Future {
            MySqlConnector.executeUpdate(conn, requestBody.sql)
          }
        }
      })
    } else {
      FutureUtils.requestFail(errMsg)
    }
  }

}

object SqlRequestInvokerActor {

  def props() = Props(new SqlRequestInvokerActor())
}
