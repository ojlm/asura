package asura.core.sql.actor

import java.sql.Connection
import java.util

import akka.actor.Props
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import asura.common.actor.BaseActor
import asura.core.es.model.SqlRequest.SqlRequestBody
import asura.core.sql.actor.MySqlConnectionCacheActor.GetConnectionMessage
import asura.core.sql.{MySqlConnector, SqlConfig, SqlParserUtils, SqlToExecute}
import asura.core.{CoreConfig, ErrorMessages}

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
    futConn.flatMap(conn => {
      Future {
        val statements = SqlParserUtils.getStatements(requestBody.sql)
        if (statements.size > 1) {
          val results = new util.ArrayList[Object]()
          statements.foreach(statement => {
            results.add(executeSql(conn, statement))
          })
          results
        } else if (statements.size == 1) {
          executeSql(conn, statements(0))
        } else {
          ErrorMessages.error_InvalidRequestParameters.toFutureFail
        }
      }
    })
  }

  private def executeSql(conn: Connection, sql: SqlToExecute): Object = {
    if (sql.isSelect) {
      MySqlConnector.executeQuery(conn, sql.sql)
    } else {
      MySqlConnector.executeUpdate(conn, sql.sql)
    }
  }
}

object SqlRequestInvokerActor {

  def props() = Props(new SqlRequestInvokerActor())
}
